package io.zenandroid.onlinego.ui.composables

import androidx.compose.ui.geometry.Rect
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle

private val typeface = FontMgr.default.matchFamilyStyle(".AppleSystemUIFont", FontStyle.NORMAL)

internal actual fun textInkBounds(text: String, textSize: Float): Rect {
  val bounds = Font(typeface, textSize).measureText(text)
  return Rect(bounds.left, bounds.top, bounds.right, bounds.bottom)
}
