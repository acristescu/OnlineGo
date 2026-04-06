package io.zenandroid.onlinego.ui.screens.face2face.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceToFaceLanDiscoveryManagerTest {
  @Test
  fun `builds readable service name from device and session`() {
    val serviceName = buildLanServiceName(
      deviceName = "Pixel 7 Pro",
      sessionId = "12345678-abcdef",
    )

    assertEquals("OnlineGo F2F Pixel 7 Pro 123456", serviceName)
  }

  @Test
  fun `extracts display name from service name`() {
    assertEquals(
      "Pixel 7 Pro",
      displayNameFromLanServiceName("OnlineGo F2F Pixel 7 Pro 123456"),
    )
  }

  @Test
  fun `falls back to host when service name is unexpected`() {
    assertEquals("Host", displayNameFromLanServiceName("OnlineGo F2F"))
  }

  @Test
  fun `caps service name length for long device names`() {
    val serviceName = buildLanServiceName(
      deviceName = "Pixel 9 Pro XL Developer Preview Build Extra Long Device Name",
      sessionId = "12345678-abcdef",
    )

    assertTrue(serviceName.length <= 63)
    assertEquals("123456", serviceName.takeLast(6))
  }
}
