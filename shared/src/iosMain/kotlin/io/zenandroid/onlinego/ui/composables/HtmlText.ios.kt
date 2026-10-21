package io.zenandroid.onlinego.ui.composables

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLinkStyles

private val lineBreak = Regex("(?i)<br\\s*/?>|</p>")
private val tag = Regex("<[^>]*>")
private val entities = listOf(
  "&lt;" to "<",
  "&gt;" to ">",
  "&quot;" to "\"",
  "&#39;" to "'",
  "&nbsp;" to " ",
  "&amp;" to "&",
)

internal actual fun htmlToAnnotatedString(
  html: String,
  linkStyles: TextLinkStyles
): AnnotatedString =
  AnnotatedString(
    entities.fold(html.replace(lineBreak, "\n").replace(tag, "")) { text, (entity, char) ->
      text.replace(entity, char)
    }
  )
