package io.zenandroid.onlinego.ui.screens.face2face.session

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class FaceToFaceLanJoinTarget(
  val host: String,
  val port: Int,
)

fun parseFaceToFaceLanJoinTarget(
  raw: String,
  defaultPort: Int = FaceToFaceLanConnectionManager.DEFAULT_PORT,
): FaceToFaceLanJoinTarget {
  val trimmed = raw.trim()
  require(trimmed.isNotBlank()) { "Enter the host address." }

  val separatorIndex = trimmed.lastIndexOf(':')
  val singlePortSeparator = separatorIndex > 0 && separatorIndex == trimmed.indexOf(':')
  if (!singlePortSeparator) {
    return FaceToFaceLanJoinTarget(host = trimmed, port = defaultPort)
  }

  val host = trimmed.substring(0, separatorIndex).trim()
  val port = trimmed.substring(separatorIndex + 1).trim().toIntOrNull()

  require(host.isNotBlank()) { "Enter the host address." }
  require(port != null && port in 1..65535) { "Enter a valid host port." }

  return FaceToFaceLanJoinTarget(host = host, port = port)
}

fun buildFaceToFaceLanJoinErrorMessage(
  target: FaceToFaceLanJoinTarget,
  error: Throwable,
  emulatorMode: Boolean,
): String {
  val rootCause = generateSequence(error) { it.cause }.last()
  val emulatorHint = if (emulatorMode) {
    " On Android Emulator, join via 10.0.2.2 and keep adb port forwarding active."
  } else {
    ""
  }

  return when (rootCause) {
    is UnknownHostException -> "Couldn't resolve \"${target.host}\". Check the address and try again."
    is SocketTimeoutException -> "Timed out connecting to ${target.host}:${target.port}. Make sure the host is online and reachable.$emulatorHint"
    is ConnectException -> "Couldn't reach ${target.host}:${target.port}. Make sure the host is running and reachable.$emulatorHint"
    else -> "Couldn't connect to ${target.host}:${target.port}. Try again.$emulatorHint"
  }
}
