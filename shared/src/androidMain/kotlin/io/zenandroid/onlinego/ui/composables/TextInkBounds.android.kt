package io.zenandroid.onlinego.ui.composables

import android.graphics.Paint
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect

private val paints = object : ThreadLocal<Paint>() {
  override fun initialValue() = Paint(Paint.ANTI_ALIAS_FLAG)
}

internal actual fun textInkBounds(text: String, textSize: Float): Rect {
  val bounds = android.graphics.Rect()
  paints.get()!!.apply {
    this.textSize = textSize
    getTextBounds(text, 0, text.length, bounds)
  }
  return bounds.toComposeRect()
}
