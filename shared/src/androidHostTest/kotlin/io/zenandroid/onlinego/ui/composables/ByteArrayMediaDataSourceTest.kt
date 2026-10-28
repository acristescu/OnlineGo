package io.zenandroid.onlinego.ui.composables

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ByteArrayMediaDataSourceTest {

  private val source = ByteArrayMediaDataSource(byteArrayOf(1, 2, 3, 4, 5))

  @Test
  fun reportsSize() {
    assertEquals(5L, source.size)
  }

  @Test
  fun readsIntoBufferAtOffset() {
    val buffer = ByteArray(4)

    assertEquals(2, source.readAt(1, buffer, 1, 2))
    assertArrayEquals(byteArrayOf(0, 2, 3, 0), buffer)
  }

  @Test
  fun truncatesReadAtEnd() {
    val buffer = ByteArray(10)

    assertEquals(2, source.readAt(3, buffer, 0, 10))
    assertArrayEquals(byteArrayOf(4, 5), buffer.copyOf(2))
  }

  @Test
  fun signalsEndOfStream() {
    assertEquals(-1, source.readAt(5, ByteArray(1), 0, 1))
  }
}
