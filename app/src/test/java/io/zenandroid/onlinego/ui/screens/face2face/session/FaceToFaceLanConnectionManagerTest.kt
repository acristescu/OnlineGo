package io.zenandroid.onlinego.ui.screens.face2face.session

import org.junit.Assert.assertEquals
import org.junit.Test

class FaceToFaceLanConnectionManagerTest {
  @Test
  fun `selects site local wireless address ahead of tunnel and loopback`() {
    val selected = selectBestLanHostAddress(
      listOf(
        FaceToFaceLanAddressCandidate(
          interfaceName = "tailscale0",
          displayName = "Tailscale",
          hostAddress = "100.101.102.103",
          isSiteLocal = false,
          isLinkLocal = false,
          isVirtual = false,
          isPointToPoint = false,
        ),
        FaceToFaceLanAddressCandidate(
          interfaceName = "wlan0",
          displayName = "Wi-Fi",
          hostAddress = "192.168.1.42",
          isSiteLocal = true,
          isLinkLocal = false,
          isVirtual = false,
          isPointToPoint = false,
        ),
        FaceToFaceLanAddressCandidate(
          interfaceName = "lo",
          displayName = "Loopback",
          hostAddress = "127.0.0.1",
          isSiteLocal = false,
          isLinkLocal = false,
          isVirtual = false,
          isPointToPoint = false,
        ),
      )
    )

    assertEquals("192.168.1.42", selected)
  }
}
