package io.zenandroid.onlinego.ui.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

private val tagPattern = Regex("""<\s*(/?)\s*([a-zA-Z][a-zA-Z0-9]*)([^>]*)>""")
private val attributePattern = Regex("""([a-zA-Z-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'>]+))""")
private val entityPattern = Regex("""&(#[xX][0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);""")

private val blockTags = setOf("p", "div")

private val namedEntities = mapOf(
  "amp" to "&",
  "lt" to "<",
  "gt" to ">",
  "quot" to "\"",
  "apos" to "'",
  "nbsp" to " ",
)

private val htmlColors = mapOf(
  "black" to 0xFF000000,
  "darkgray" to 0xFF444444,
  "darkgrey" to 0xFF444444,
  "gray" to 0xFF888888,
  "grey" to 0xFF888888,
  "lightgray" to 0xFFCCCCCC,
  "lightgrey" to 0xFFCCCCCC,
  "white" to 0xFFFFFFFF,
  "red" to 0xFFFF0000,
  "green" to 0xFF00FF00,
  "lime" to 0xFF00FF00,
  "blue" to 0xFF0000FF,
  "yellow" to 0xFFFFFF00,
  "cyan" to 0xFF00FFFF,
  "aqua" to 0xFF00FFFF,
  "magenta" to 0xFFFF00FF,
  "fuchsia" to 0xFFFF00FF,
  "maroon" to 0xFF800000,
  "navy" to 0xFF000080,
  "olive" to 0xFF808000,
  "purple" to 0xFF800080,
  "silver" to 0xFFC0C0C0,
  "teal" to 0xFF008080,
)

internal actual fun htmlToAnnotatedString(
  html: String,
  linkStyles: TextLinkStyles
): AnnotatedString = HtmlConverter(linkStyles).convert(html)

private class HtmlConverter(private val linkStyles: TextLinkStyles) {
  private class OpenTag(val name: String, val start: Int, val attributes: Map<String, String>)

  private val builder = AnnotatedString.Builder()
  private val openTags = mutableListOf<OpenTag>()
  private var lastChar = '\n'

  fun convert(html: String): AnnotatedString {
    var position = 0
    tagPattern.findAll(html).forEach { match ->
      appendText(html.substring(position, match.range.first))
      position = match.range.last + 1
      val (slash, rawName, attributes) = match.destructured
      val name = rawName.lowercase()
      if (name in blockTags) {
        breakLine()
      } else if (slash.isEmpty()) {
        openTag(name, attributes)
      } else {
        openTags.indexOfLast { it.name == name }
          .takeIf { it >= 0 }
          ?.let { close(openTags.removeAt(it)) }
      }
    }
    appendText(html.substring(position))
    openTags.asReversed().forEach(::close)
    return builder.toAnnotatedString()
  }

  private fun openTag(name: String, attributes: String) {
    when (name) {
      "br" -> append('\n')
      "img" -> Unit
      else -> openTags += OpenTag(name, builder.length, parseAttributes(attributes))
    }
  }

  private fun close(tag: OpenTag) {
    val end = builder.length
    if (tag.start == end) return
    when (tag.name) {
      "b" -> builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), tag.start, end)
      "i" -> builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), tag.start, end)
      "u" -> builder.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), tag.start, end)
      "s" -> builder.addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), tag.start, end)
      "a" -> tag.attributes["href"]?.let {
        builder.addLink(LinkAnnotation.Url(it, linkStyles), tag.start, end)
      }
      "font" -> tag.attributes["color"]?.let(::htmlColor)?.let {
        builder.addStyle(SpanStyle(color = Color(it)), tag.start, end)
      }
    }
  }

  private fun appendText(raw: String) {
    decodeEntities(raw).forEach { char ->
      if (char == ' ' || char == '\n') {
        if (lastChar != ' ' && lastChar != '\n') append(' ')
      } else {
        append(char)
      }
    }
  }

  private fun breakLine() {
    if (builder.length > 0 && lastChar != '\n') append('\n')
  }

  private fun append(char: Char) {
    builder.append(char)
    lastChar = char
  }
}

private fun parseAttributes(attributes: String): Map<String, String> =
  attributePattern.findAll(attributes).associate { match ->
    val (name, doubleQuoted, singleQuoted, unquoted) = match.destructured
    name.lowercase() to decodeEntities(doubleQuoted + singleQuoted + unquoted)
  }

private fun decodeEntities(text: String): String =
  entityPattern.replace(text) { match ->
    val entity = match.groupValues[1]
    val codePoint = when {
      entity.startsWith("#x", ignoreCase = true) -> entity.drop(2).toIntOrNull(16)
      entity.startsWith("#") -> entity.drop(1).toIntOrNull()
      else -> null
    }
    when {
      codePoint != null && codePoint <= 0xFFFF -> codePoint.toChar().toString()
      codePoint == null -> namedEntities[entity.lowercase()] ?: match.value
      else -> match.value
    }
  }

private fun htmlColor(value: String): Long? =
  htmlColors[value.lowercase()]
    ?: value.takeIf { it.startsWith("#") }?.drop(1)?.toLongOrNull(16)?.let { it or 0xFF000000 }
