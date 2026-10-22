package io.zenandroid.onlinego.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.MessageDigest

class ChangelogHashTest {

  @Test
  fun matchesTheHashEarlierVersionsStored() {
    val items =
      listOf("Portuguese translation — obrigado!", "Board: centred labels", "Ελληνικά, 日本語")
    val legacy = MessageDigest.getInstance("MD5")
      .digest(items.joinToString("\n").toByteArray(Charsets.UTF_8))
      .fold("") { str, it -> str + "%02x".format(it) }

    assertEquals(legacy, changelogHash(items))
  }
}
