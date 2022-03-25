package io.zenandroid.onlinego.ai

import android.util.Log
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.gamelogic.Util
import java.io.*

@Deprecated("This worked, but was never released. If we ever decide to bring back that horrible monster called GTP, this is a good stat though.")
object LeelaZeroService {

//    var executor: Executor? = null
    var started = false
    var process: Process? = null
    var writer: OutputStreamWriter? = null
    var reader: BufferedReader? = null

    private val leelaLogSubject = PublishSubject.create<String>()
    val leelaLog = leelaLogSubject.hide()

    @Throws(IOException::class)
    @Synchronized
    fun startEngine() {
        if(started) {
            return
        }
        process = ProcessBuilder("./leelaz.so",
                "--cpu-only",
                "--lagbuffer","0",
//                "--playouts","1",
                "-r","0",
                "--gtp",
                "--noponder",
                "-w","/data/local/tmp/0c4ade7958b3441b9f13ecd06f99f11540e5e4d595127f7f3eb73f0c02b58d92.gz")
//        process = ProcessBuilder("ls", "-al")
                .redirectErrorStream(true).apply {
                    environment()["LD_LIBRARY_PATH"] = "."
                }
                .directory(File(OnlineGoApplication.instance.applicationInfo.nativeLibraryDir))
                .start()
                .apply {
                    reader = BufferedReader(InputStreamReader(inputStream))
                    writer = OutputStreamWriter(outputStream)
//                    executor = Executors.newSingleThreadScheduledExecutor().also {
//                        it.execute { read(reader)}
//                    }
                    sendCommand("name") {
                        if(it == "= Leela Zero") {
                            started = true
                        }
                    }
                    if(started) {
                        sendCommand("lz-setoption name playouts value 1")
//                        sendCommand("time_settings 0 2 1")
//                        sendCommand("genmove b") {
//                            if (it.startsWith("=")) {
//                                Log.v("LeelaZeroService", "got move `${it.substring(2)}`")
//                            }
//                        }
//                        sendCommand("play w Q4")
//                        sendCommand("genmove b") {
//                            when {
//                                it.startsWith("=") -> {
//                                    Log.v("LeelaZeroService", "got move `${it.substring(2)}`")
//                                }
//                                it.startsWith("?") -> {
//                                    Log.v("LeelaZeroService", "got error `$it`")
//                                }
//                            }
//                        }
                    } else {
                        Log.e("LeelaZeroService", "Could not start Leela Zero")
                    }
                }
    }

    fun clearBoard() {
        sendCommand("clear_board")
    }

    fun setSecondsPerMove(seconds: Int) {
        sendCommand("time_settings 0 $seconds 1")
    }

    fun setPosition(pos: Position) {
        val moves = mutableListOf<Cell>()
        var currentPos: Position? = pos
        while (currentPos?.lastMove != null) {
            moves.add(currentPos.lastMove!!)
//            currentPos = currentPos.parentPosition
        }
        var blackToMove = currentPos?.nextToMove != StoneType.WHITE
        moves.reversed()
                .map { Util.getGTPCoordinates(it, pos.boardHeight) }
                .forEach {
                    val color = if(blackToMove) "black" else "white"
                    sendCommand("play $color $it")
                    blackToMove = !blackToMove
                }
//        sendCommand("showboard")
    }

    fun genmove(side: StoneType): Observable<String> =
        Observable.create { emitter ->
            sendCommand("genmove $side") {
                emitter.onNext(it)
            }
        }

    @Synchronized
    fun stopEngine() {
        if(!started) {
            return
        }
        sendCommand("quit")
        process?.waitFor()
        process = null
        started = false
    }

    @Synchronized
    private fun sendCommand(command: String, callback: ((String) -> Unit)? = null) {
        Log.v("LeelaZeroService", "Sending: `$command`")
        writer?.let {
            leelaLogSubject.onNext(">>> $command")
            it.write(command)
            it.write("\n")
            it.flush()
        }
        reader?.let {
            while (true) {
                val line = it.readLine() ?: break
                leelaLogSubject.onNext(line)
                Log.v("LeelaZeroService", "Response to `$command`: $line")
                callback?.invoke(line)
                if(line.startsWith("=") || line.startsWith("?")) {
                    break
                }
            }
        }
        Log.v("LeelaZeroService", "`$command` complete")
    }

    fun playMove(side: StoneType, coordinate: Cell, boardSize: Int) {
        val gtpMove = Util.getGTPCoordinates(coordinate, boardSize)
        sendCommand("play $side $gtpMove")
    }

    fun undoMove() {
        sendCommand("undo")
    }

    fun setFixedHandicap(handicap: Int, boardSize: Int): List<Cell> {
        val handicapStones = mutableListOf<Cell>()
        sendCommand("fixed_handicap $handicap") {
            if(it.startsWith("=")) {
                it.split(" ")
                        .filter { it != "=" }
                        .forEach {
                            handicapStones.add(Util.getCoordinatesFromGTP(it, boardSize))
                        }
            }
        }
        return handicapStones
    }

//    private fun read(reader: InputStreamReader) {
//        Log.v("LeelaZeroService", "Started reading")
//        reader.forEachLine {
//            Log.v("LeelaZeroService", it)
//        }
//        Log.v("LeelaZeroService", "Stopped reading")
//    }
}