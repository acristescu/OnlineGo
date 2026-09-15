package io.zenandroid.onlinego.ai

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.katago.KataGoResponse
import io.zenandroid.onlinego.data.model.katago.KataGoResponse.ErrorResponse
import io.zenandroid.onlinego.data.model.katago.KataGoResponse.Response
import io.zenandroid.onlinego.data.model.katago.OverrideSettings
import io.zenandroid.onlinego.data.model.katago.Query
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Stack
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.ZipFile

object KataGoAnalysisEngine {
  var started = false
    private set
  var shouldShutDown = false
    private set
  private var process: Process? = null
  private var writer: OutputStreamWriter? = null
  private var reader: BufferedReader? = null
  private var requestIDX: AtomicLong = AtomicLong(0)
  private val queryAdapter =
    Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(Query::class.java)
  private val responseAdapter =
    Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(Response::class.java)
  private val errorAdapter =
    Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(ErrorResponse::class.java)
  private val responseFlow = MutableSharedFlow<KataGoResponse>(extraBufferCapacity = 64)

  // Callers also raise maxVisits to at least this many - KataGo spins up this many search
  // threads per query regardless of budget, wasting the rest otherwise.
  val searchThreads = Runtime.getRuntime().availableProcessors()
  private val filesDir = OnlineGoApplication.instance.filesDir
  private val netFile = File(filesDir, "katagonet.gz")
  private val humanNetFile = File(filesDir, "katagohumannet.gz")
  private val cfgFile = File(filesDir, "katago.cfg")

  @Throws(IOException::class)
  @Synchronized
  fun start() {
    shouldShutDown = false
    if (started) {
      return
    }
    ensureResourcesAreUnpacked()
    process = ProcessBuilder(
      "./libkatago.so",
      "analysis",
      "-model", netFile.absolutePath,
      "-human-model", humanNetFile.absolutePath,
      "-config", cfgFile.absolutePath
    )
      .apply { environment()["LD_LIBRARY_PATH"] = "." }
      .directory(File(OnlineGoApplication.instance.applicationInfo.nativeLibraryDir))
      .start()
      .apply {
        reader = BufferedReader(InputStreamReader(inputStream))
        writer = OutputStreamWriter(outputStream)
        val errorReader = BufferedReader(InputStreamReader(errorStream))
        val errors = StringBuffer()

        reader?.let {
          while (true) {
            val line = errorReader.readLine() ?: break
            if (line.startsWith("KataGo v")) {
              continue
            } else if (line == "Started, ready to begin handling requests") {
              requestIDX = AtomicLong(0)
              started = true
              break
            } else {
              Log.e("KataGoAnalysisEngine", line)
              errors.appendLine(line)
            }
          }
        }

        if (started) {
          Thread {
            while (true) {
              val line = reader?.readLine() ?: break
              try {
                if (line.contains("\"error\":") || line.contains("\"warning\":")) {
                  Log.e("KataGoAnalysisEngine", line)
                  recordException(Exception("Katago: $line"))
                  errorAdapter.fromJson(line)?.let {
                    responseFlow.tryEmit(it)
                  }
                } else {
                  Log.d("KataGoAnalysisEngine", line)
                  FirebaseCrashlytics.getInstance().log("KATAGO < $line")
                  responseAdapter.fromJson(line)?.let {
                    responseFlow.tryEmit(it)
                  }
                }
              } catch (e: Exception) {
                Log.e("KataGoAnalysisEngine", "Failed to parse KataGo line: $line", e)
                recordException(e)
              }
            }
            Log.d("KataGoAnalysisEngine", "End of input, killing reader thread")
            FirebaseCrashlytics.getInstance().log("KATAGO < End of input, killing reader thread")
            started = false
          }.start()
        } else {
          Log.e("KataGoAnalysisEngine", "Could not start KataGo")
          recordException(Exception("Could not start KataGo $errors"))
          throw RuntimeException("Could not start KataGo")
        }
      }
  }

  @Synchronized
  fun stop() {
    shouldShutDown = true
    if (!started) {
      return
    }
    Thread {
      Thread.sleep(2000)
      synchronized(KataGoAnalysisEngine) {
        if (shouldShutDown && started) {
          try {
            writer?.close()
            process?.waitFor()
          } catch (t: Throwable) {
            process?.destroy()
          }
          process = null
          started = false
          shouldShutDown = false
        }
      }
    }.start()
  }

  suspend fun analyzeMoveSequence(
    sequence: List<Position>,
    komi: Float? = null,
    maxVisits: Int? = null,
    includeOwnership: Boolean? = null,
    includeMovesOwnership: Boolean? = null,
    includePolicy: Boolean? = null,
    overrideSettings: OverrideSettings? = null
  ): Response {
    val id = generateId()

    val initialPosition = mutableSetOf<List<String>>()
    val history = Stack<List<String>>()
    sequence.map { pos ->
      if (pos.lastMove == null) {
        initialPosition.addAll(pos.whiteStones.map {
          listOf("W", Util.getGTPCoordinates(it, pos.boardHeight))
        })
        initialPosition.addAll(pos.blackStones.map {
          listOf("B", Util.getGTPCoordinates(it, pos.boardHeight))
        })
      } else {
        val lastPlayer = if (pos.lastPlayerToMove == StoneType.BLACK) "B" else "W"
        val lastMove = Util.getGTPCoordinates(pos.lastMove, pos.boardHeight)
        history.push(listOf(lastPlayer, lastMove))
      }
    }

    val query = Query(
      id = id,
      boardXSize = sequence.firstOrNull()?.boardWidth ?: 19,
      boardYSize = sequence.firstOrNull()?.boardHeight ?: 19,
      includeOwnership = includeOwnership,
      includeMovesOwnership = includeMovesOwnership,
      includePolicy = includePolicy,
      initialStones = initialPosition.toList(),
      komi = komi,
      maxVisits = maxVisits,
      overrideSettings = overrideSettings,
      moves = history,
      rules = "japanese"
    )

    val stringQuery = queryAdapter.toJson(query)

    Log.d("KataGoAnalysisEngine", stringQuery)
    FirebaseCrashlytics.getInstance().log("KATAGO> $stringQuery")
    writer?.apply {
      write(stringQuery + "\n")
      flush()
    }

    val response = responseFlow.first { it.id == id }
    if (response is ErrorResponse) {
      throw RuntimeException(response.error)
    }
    return response as Response
  }

  private fun generateId() = requestIDX.incrementAndGet().toString()

  private fun ensureResourcesAreUnpacked() {
    unpackResource("katago.net", netFile)
    unpackResource("katago_human.net", humanNetFile)
    writeConfigWithThreadCount()
  }

  // Thread pool size is fixed at KataGo process startup, unlike humanSLProfile - it can't
  // be set via per-query overrideSettings, so the cfg has to be regenerated per engine start.
  private fun writeConfigWithThreadCount() {
    val template = OnlineGoApplication.instance.assets.open("katago.cfg")
      .bufferedReader().use { it.readText() }
    val configured = template.replace(
      Regex("""(?m)^numSearchThreadsPerAnalysisThread\s*=.*$"""),
      "numSearchThreadsPerAnalysisThread = $searchThreads"
    )
    cfgFile.writeText(configured)
  }

  // Reads the real size from the APK's zip central directory - AssetManager.openFd() fails
  // on AAPT-compressed assets, and this avoids a hardcoded byte count to keep in sync by hand.
  private fun expectedAssetSize(srcName: String): Long {
    val apkPath = OnlineGoApplication.instance.applicationInfo.sourceDir
    ZipFile(apkPath).use { zip ->
      return zip.getEntry("assets/$srcName")?.size
        ?: throw IOException("Asset '$srcName' not found in APK at $apkPath")
    }
  }

  private fun unpackResource(srcName: String, destFile: File) {
    val assets = OnlineGoApplication.instance.assets
    val expectedSize = expectedAssetSize(srcName)
    if (!destFile.exists() || destFile.length() != expectedSize) {
      destFile.delete()
      assets.open(srcName).use { input ->
        destFile.outputStream().use { output -> input.copyTo(output) }
      }
    }
  }
}