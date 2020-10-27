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
import io.zenandroid.onlinego.data.model.katago.Query
import io.zenandroid.onlinego.data.model.katago.Response
import io.zenandroid.onlinego.data.model.katago.RootInfo
import io.zenandroid.onlinego.gamelogic.Util
import java.io.*
import java.lang.RuntimeException
import java.util.*
import java.util.concurrent.atomic.AtomicLong

object KataGoAnalysisEngine {
    var started = false
        private set
    private var process: Process? = null
    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null
    private var requestIDX: AtomicLong = AtomicLong(0)
    private val queryAdapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(Query::class.java)
    private val responseAdapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(Response::class.java)
    private val responseSubject = PublishSubject.create<Response>()
    private val filesDir = OnlineGoApplication.instance.filesDir
    private val netFile = File(filesDir, "katagonet.gz")
    private val cfgFile = File(filesDir, "katago.cfg")

    @Throws(IOException::class)
    @Synchronized
    fun startEngine() {
        if (started) {
            return
        }
        ensureResourcesAreUnpacked()
        process = ProcessBuilder(
                "./libkatago.so",
                "analysis",
                "-model", netFile.absolutePath,
                "-config", cfgFile.absolutePath)
                .apply { environment()["LD_LIBRARY_PATH"] = "." }
                .directory(File(OnlineGoApplication.instance.applicationInfo.nativeLibraryDir))
                .start()
                .apply {
                    reader = BufferedReader(InputStreamReader(inputStream))
                    writer = OutputStreamWriter(outputStream)
                    val errorReader = BufferedReader(InputStreamReader(errorStream))
                    val errors = StringBuffer()

                    reader?.let {
                        while(true) {
                            val line = errorReader.readLine() ?: break
                            if(line.startsWith("KataGo v")) {
                                continue
                            } else if(line == "Started, ready to begin handling requests") {
                                requestIDX = AtomicLong(0)
                                started = true
                                break
                            } else {
                                Log.e("KataGoAnalysisEngine", line)
                                errors.appendln(line)
                            }
                        }
                    }

                    if(started) {
                        Thread {
                            while(true) {
                                val line = reader?.readLine() ?: break
                                Log.d("KataGoAnalysisEngine", line)
                                if(line.startsWith("{\"error\"") || line.startsWith("{\"warning\":\"WARNING_MESSAGE\"")) {
                                    Log.e("KataGoAnalysisEngine", line)
                                    FirebaseCrashlytics.getInstance().recordException(Exception("Katago: $line"))
                                } else {
                                    responseAdapter.fromJson(line)?.let {
                                        responseSubject.onNext(it)
                                    }
                                }
                            }
                            Log.d("KataGoAnalysisEngine", "End of input, killing reader thread")
                        }.start()
                    } else {
                        Log.e("KataGoAnalysisEngine", "Could not start KataGo")
                        FirebaseCrashlytics.getInstance().recordException(Exception("Could not start KataGo $errors"))
                        throw RuntimeException("Could not start KataGo")
                    }
                }
    }

    @Synchronized
    fun stopEngine() {
        if(!started) {
            return
        }
        writer?.close()
        process?.waitFor()
        process = null
        started = false
    }

    fun analyzePosition(
            pos: Position,
            komi: Float? = null,
            maxVisits: Int? = null,
            includeOwnership: Boolean? = null,
            includeMovesOwnership: Boolean? = null,
            includePolicy: Boolean? = null
    ): Single<Response> {

        val id = generateId()
        return responseSubject
                .filter { it.id == id }
                .firstOrError()
                .doOnSubscribe {
                    var cursor: Position? = pos
                    val history = Stack<List<String>>()
                    while(true) {
                        if(cursor?.parentPosition == null) {
                            break
                        }
                        cursor.lastMove?.let {
                            val lastPlayer = if(cursor?.lastPlayerToMove == StoneType.BLACK) "B" else "W"
                            val lastMove = Util.getGTPCoordinates(it, pos.boardSize)
                            history.push(listOf(lastPlayer, lastMove))
                        }
                        cursor = cursor.parentPosition
                    }

                    val initialPosition = mutableSetOf<List<String>>()
                    cursor?.whiteStones?.forEach {
                        initialPosition.add(listOf("W", Util.getGTPCoordinates(it, pos.boardSize)))
                    }
                    cursor?.blackStones?.forEach {
                        initialPosition.add(listOf("B", Util.getGTPCoordinates(it, pos.boardSize)))
                    }

                    val query = Query(
                            id = id,
                            boardXSize = pos.boardSize,
                            boardYSize = pos.boardSize,
                            includeOwnership = includeOwnership,
                            includeMovesOwnership = includeMovesOwnership,
                            includePolicy = includePolicy,
                            initialStones = initialPosition.toList(),
                            komi = komi,
                            maxVisits = maxVisits,
                            moves = history.reversed(),
                            rules = "japanese"
                    )

                    val stringQuery = queryAdapter.toJson(query)

                    Log.d("KataGoAnalysisEngine", stringQuery)
                    FirebaseCrashlytics.getInstance().log("KATAGO> $stringQuery")
                    writer?.apply {
                        write(stringQuery)
                        write("\n")
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
        if(!destFile.exists() || destFile.length() != expectedSize) {
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