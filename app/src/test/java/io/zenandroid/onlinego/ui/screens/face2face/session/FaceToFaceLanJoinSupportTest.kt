package io.zenandroid.onlinego.ui.screens.face2face.session

import java.net.BindException
import java.net.ConnectException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceToFaceLanJoinSupportTest {
  @Test
  fun `parses host without explicit port`() {
    val target = parseFaceToFaceLanJoinTarget("10.0.2.2")

    assertEquals("10.0.2.2", target.host)
    assertEquals(FaceToFaceLanConnectionManager.DEFAULT_PORT, target.port)
  }

  @Test
  fun `parses host with explicit port`() {
    val target = parseFaceToFaceLanJoinTarget("10.0.2.2:5555")

    assertEquals("10.0.2.2", target.host)
    assertEquals(5555, target.port)
  }

  @Test
  fun `rejects invalid port`() {
    val error = runCatching {
      parseFaceToFaceLanJoinTarget("10.0.2.2:abc")
    }.exceptionOrNull()

    assertEquals("Enter a valid host port.", error?.message)
  }

  @Test
  fun `connect exception gets emulator specific hint`() {
    val message = buildFaceToFaceLanJoinErrorMessage(
      target = FaceToFaceLanJoinTarget("10.0.2.15", 45123),
      error = ConnectException("ECONNREFUSED"),
      emulatorMode = true,
    )

    assertTrue(message.contains("Couldn't reach 10.0.2.15:45123"))
    assertTrue(message.contains("10.0.2.2"))
  }

  @Test
  fun `bind exception gets actionable host retry message`() {
    val message = buildFaceToFaceLanHostErrorMessage(
      port = 45123,
      error = BindException("bind failed: EADDRINUSE (Address already in use)")
    )

    assertTrue(message.contains("Couldn't start hosting on port 45123"))
    assertTrue(message.contains("already in use"))
    assertTrue(message.contains("wait a moment and try again"))
  }
}
