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

  @Test
  fun `falls back to ethernet when wifi is unavailable`() {
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
          interfaceName = "enp3s0",
          displayName = "Ethernet",
          hostAddress = "192.168.0.55",
          isSiteLocal = true,
          isLinkLocal = false,
          isVirtual = false,
          isPointToPoint = false,
        ),
      )
    )

    assertEquals("192.168.0.55", selected)
  }

  @Test
  fun `falls back to loopback when no usable lan interface exists`() {
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
          interfaceName = "docker0",
          displayName = "Docker bridge",
          hostAddress = "172.17.0.1",
          isSiteLocal = true,
          isLinkLocal = false,
          isVirtual = true,
          isPointToPoint = false,
        ),
      )
    )

    assertEquals("127.0.0.1", selected)
  }
}
