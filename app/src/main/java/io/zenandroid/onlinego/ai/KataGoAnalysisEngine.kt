package io.zenandroid.onlinego.ai

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.katago.KataGoResponse
import io.zenandroid.onlinego.data.model.katago.KataGoResponse.*
import io.zenandroid.onlinego.data.model.katago.Query
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.utils.recordException
import org.koin.core.context.GlobalContext
import java.io.*
import java.util.*
import java.util.concurrent.atomic.AtomicLong

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
  private val responseSubject = PublishSubject.create<KataGoResponse>()
  private val filesDir = OnlineGoApplication.instance.filesDir
  private val netFile = File(filesDir, "katagonet.gz")
  private val cfgFile = File(filesDir, "katago.cfg")

  private val settingsRepository: SettingsRepository by GlobalContext.get().inject()

  @Throws(IOException::class)
  @Synchronized
  fun startEngine() {
    shouldShutDown = false
    if (started) {
      return
    }
    ensureResourcesAreUnpacked()
    process = ProcessBuilder(
      "./libkatago.so",
      "analysis",
      "-model", netFile.absolutePath,
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
              if (line.startsWith("{\"error\"") || line.startsWith("{\"warning\":\"WARNING_MESSAGE\"")) {
                Log.e("KataGoAnalysisEngine", line)
                recordException(Exception("Katago: $line"))
                errorAdapter.fromJson(line)?.let {
                  responseSubject.onNext(it)
                }
              } else {
                Log.d("KataGoAnalysisEngine", line)
                FirebaseCrashlytics.getInstance().log("KATAGO < $line")
                responseAdapter.fromJson(line)?.let {
                  responseSubject.onNext(it)
                }
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
  fun stopEngine() {
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

  fun analyzeMoveSequence(
    sequence: List<Position>,
    komi: Float? = null,
    maxVisits: Int? = settingsRepository.maxVisits,
    includeOwnership: Boolean? = null,
    includeMovesOwnership: Boolean? = null,
    includePolicy: Boolean? = null
  ): Single<Response> {

    val id = generateId()
    return responseSubject
      .filter { it.id == id }
      .firstOrError()
      .map {
        if (it is ErrorResponse) {
          throw RuntimeException(it.error)
        } else {
          it as Response
        }
      }.doOnSubscribe {
        val initialPosition = mutableSetOf<List<String>>()
        val history = Stack<List<String>>()
        sequence.map { pos ->
          if (pos.lastMove == null) {
            initialPosition.addAll(pos.whiteStones.map {
              listOf(
                "W",
                Util.getGTPCoordinates(it, pos.boardHeight)
              )
            })
            initialPosition.addAll(pos.blackStones.map {
              listOf(
                "B",
                Util.getGTPCoordinates(it, pos.boardHeight)
              )
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
      }
  }

  private fun generateId() = requestIDX.incrementAndGet().toString()

  private const val KATAGO_NET_SIZE = 36948927L
  private const val KATAGO_CFG_SIZE = 543L

  private fun ensureResourcesAreUnpacked() {
    unpackResource("katago.net", netFile, KATAGO_NET_SIZE)
    unpackResource("katago.cfg", cfgFile, KATAGO_CFG_SIZE)
  }

  private fun unpackResource(srcName: String, destFile: File, expectedSize: Long) {
    val assets = OnlineGoApplication.instance.assets
    if (!destFile.exists() || destFile.length() != expectedSize) {
      destFile.delete()
      assets.open(srcName).apply {
        val out = destFile.outputStream()
        copyTo(out)
        out.close()
        close()
      }
    }
  }
}