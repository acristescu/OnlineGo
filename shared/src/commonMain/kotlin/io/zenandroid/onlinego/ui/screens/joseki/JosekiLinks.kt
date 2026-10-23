package io.zenandroid.onlinego.ui.screens.joseki

import io.ktor.http.parseUrl
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser

internal sealed interface JosekiLink {
  data class Position(val nodeId: Long?) : JosekiLink
  data object Video : JosekiLink
  data object External : JosekiLink
}

private const val POSITION_PREFIX = "Position:"
private val nodeIdPattern = Regex("\\d+")
private val videoHosts = setOf("youtube.com", "www.youtube.com", "youtu.be")

internal fun josekiLinkOf(destination: String): JosekiLink {
  if (destination.startsWith(POSITION_PREFIX)) {
    return JosekiLink.Position(destination.removePrefix(POSITION_PREFIX).toLongOrNull())
  }
  if (nodeIdPattern.matches(destination)) {
    return JosekiLink.Position(destination.toLongOrNull())
  }
  val url = parseUrl(destination) ?: return JosekiLink.External
  return when {
    url.host.endsWith("online-go.com") && url.encodedPath.startsWith("/joseki/") ->
      JosekiLink.Position(url.segments.lastOrNull()?.toLongOrNull())

    url.host in videoHosts -> JosekiLink.Video
    else -> JosekiLink.External
  }
}

internal fun labelJosekiLinks(markdown: String, videoLabel: String, externalLabel: String): String {
  val source: CharSequence = markdown
  val tree = MarkdownParser(
    CommonMarkFlavourDescriptor(),
    cancellationToken = CancellationToken.NonCancellable
  )
    .buildMarkdownTreeFromString(source)
  val insertions = buildList {
    tree.forEachInlineLink { link ->
      val text = link.children.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }
      val destination =
        link.children.firstOrNull { it.type == MarkdownElementTypes.LINK_DESTINATION }
      if (text != null && destination != null) {
        val label =
          when (josekiLinkOf(destination.getTextInNode(markdown).trim('<', '>').toString())) {
            is JosekiLink.Position -> null
            JosekiLink.Video -> videoLabel
            JosekiLink.External -> externalLabel
          }
        if (label != null) add(text.endOffset - 1 to " $label")
      }
    }
  }
  return insertions.foldRight(StringBuilder(markdown)) { (offset, label), result ->
    result.insert(offset, label)
  }.toString()
}

private fun ASTNode.forEachInlineLink(action: (ASTNode) -> Unit) {
  if (type == MarkdownElementTypes.INLINE_LINK) action(this)
  children.forEach { it.forEachInlineLink(action) }
}
