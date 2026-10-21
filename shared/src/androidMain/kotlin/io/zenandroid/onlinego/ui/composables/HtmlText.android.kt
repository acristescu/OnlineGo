package io.zenandroid.onlinego.ui.composables

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml

internal actual fun htmlToAnnotatedString(
  html: String,
  linkStyles: TextLinkStyles
): AnnotatedString =
  AnnotatedString.fromHtml(html, linkStyles)
