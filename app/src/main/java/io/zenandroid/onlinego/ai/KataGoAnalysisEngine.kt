package io.zenandroid.onlinego.ai

import android.content.Context
import co.touchlab.kermit.Logger
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.katago.KataGoResponse
import io.zenandroid.onlinego.data.model.katago.KataGoResponse.ErrorResponse
import io.zenandroid.onlinego.data.model.katago.KataGoResponse.Response
import io.zenandroid.onlinego.data.model.katago.OverrideSettings
import io.zenandroid.onlinego.data.model.katago.Query
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.utils.appJson
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

class KataGoAnalysisEngine(private val context: Context) {
  var started = false
    private set
  var shouldShutDown = false
    private set
  private var process: Process? = null
  private var writer: OutputStreamWriter? = null
  private var reader: BufferedReader? = null
  private var requestIDX: AtomicLong = AtomicLong(0)
  private val responseFlow = MutableSharedFlow<KataGoResponse>(extraBufferCapacity = 64)

  // Callers also raise maxVisits to at least this many - KataGo spins up this many search
  // threads per query regardless of budget, wasting the rest otherwise.
  val searchThreads = Runtime.getRuntime().availableProcessors()
  private val netFile by lazy { File(context.filesDir, "katagonet.gz") }
  private val humanNetFile by lazy { File(context.filesDir, "katagohumannet.gz") }
  private val cfgFile by lazy { File(context.filesDir, "katago.cfg") }

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
      .directory(File(context.applicationInfo.nativeLibraryDir))
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
              Logger.e(line, tag = "KataGoAnalysisEngine")
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
                  Logger.e(line, tag = "KataGoAnalysisEngine")
                  recordException(Exception("Katago: $line"))
                  responseFlow.tryEmit(appJson.decodeFromString<ErrorResponse>(line))
                } else {
                  Logger.i("< $line", tag = "KataGoAnalysisEngine")
                  responseFlow.tryEmit(appJson.decodeFromString<Response>(line))
                }
              } catch (e: Exception) {
                Logger.e("Failed to parse KataGo line: $line", e, "KataGoAnalysisEngine")
                recordException(e)
              }
            }
            Logger.i("End of input, killing reader thread", tag = "KataGoAnalysisEngine")
            started = false
          }.start()
        } else {
          Logger.e("Could not start KataGo", tag = "KataGoAnalysisEngine")
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
      synchronized(this@KataGoAnalysisEngine) {
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

    val stringQuery = appJson.encodeToString(query)

    Logger.i("> $stringQuery", tag = "KataGoAnalysisEngine")
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
    val template = context.assets.open("katago.cfg")
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
    val apkPath = context.applicationInfo.sourceDir
    ZipFile(apkPath).use { zip ->
      return zip.getEntry("assets/$srcName")?.size
        ?: throw IOException("Asset '$srcName' not found in APK at $apkPath")
    }
  }

  private fun unpackResource(srcName: String, destFile: File) {
    val assets = context.assets
    val expectedSize = expectedAssetSize(srcName)
    if (!destFile.exists() || destFile.length() != expectedSize) {
      destFile.delete()
      assets.open(srcName).use { input ->
        destFile.outputStream().use { output -> input.copyTo(output) }
      }
    }
  }
}