package io.zenandroid.onlinego.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProcessGravatarURLTest {

  @Test
  fun `gravatar URL is resized to the exact width`() {
    assertEquals(
      "https://secure.gravatar.com/avatar/0123abcDEF?s=96&d=404",
      processGravatarURL("https://secure.gravatar.com/avatar/0123abcDEF?s=64&d=retro", 96),
    )
  }

  @Test
  fun `CDN upload is rounded up to the next rendered width`() {
    assertEquals(
      "https://b0c2ddc39d13e1c0ddad-93a52a5bc9e7cc06050c1a999beb3694.ssl.cf1.rackcdn.com/user-uploads.online-go.com/abc-128.png",
      processGravatarURL(
        "https://b0c2ddc39d13e1c0ddad-93a52a5bc9e7cc06050c1a999beb3694.ssl.cf1.rackcdn.com/user-uploads.online-go.com/abc-64.png",
        100,
      ),
    )
  }

  @Test
  fun `CDN upload wider than the largest rendering uses the largest`() {
    assertEquals(
      "https://user-uploads.online-go.com/abc-512.png",
      processGravatarURL("https://user-uploads.online-go.com/abc-32.png", 2000),
    )
  }

  @Test
  fun `other URLs pass through unchanged`() {
    assertEquals(
      "https://example.com/avatar.png",
      processGravatarURL("https://example.com/avatar.png", 64),
    )
  }

  @Test
  fun `null stays null`() {
    assertNull(processGravatarURL(null, 64))
  }
}
