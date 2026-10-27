package io.zenandroid.onlinego.ui.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HtmlTextTest {

  private val linkStyles = TextLinkStyles(SpanStyle(color = Color.Blue))

  private fun convert(html: String) = htmlToAnnotatedString(html, linkStyles)

  private fun AnnotatedString.styleAt(text: String): List<SpanStyle> {
    val start = this.text.indexOf(text)
    return spanStyles.filter { it.start == start && it.end == start + text.length }.map { it.item }
  }

  @Test
  fun keepsTheTextOfADescriptionAndDropsItsTags() {
    val text = convert(
      "<p>Black to play &amp; live</p><a href=\"https://online-go.com\">More</a><br/>&lt;end&gt;",
    ).text
    assertEquals("Black to play & live\nMore\n<end>", text)
  }

  @Test
  fun inlineTagsBecomeStylesOverTheirText() {
    val result = convert("<b>bold</b> <i>italic</i> <u>under</u> <s>struck</s>")

    assertEquals("bold italic under struck", result.text)
    assertEquals(listOf(SpanStyle(fontWeight = FontWeight.Bold)), result.styleAt("bold"))
    assertEquals(listOf(SpanStyle(fontStyle = FontStyle.Italic)), result.styleAt("italic"))
    assertEquals(listOf(SpanStyle(textDecoration = TextDecoration.Underline)), result.styleAt("under"))
    assertEquals(listOf(SpanStyle(textDecoration = TextDecoration.LineThrough)), result.styleAt("struck"))
  }

  @Test
  fun anchorsBecomeLinksWithTheGivenStyles() {
    val result = convert("""See <a href="http://senseis.xmp.net/?Seki">Seki</a>.""")

    assertEquals("See Seki.", result.text)
    val link = result.getLinkAnnotations(0, result.length).single()
    assertEquals(4, link.start)
    assertEquals(8, link.end)
    assertEquals(LinkAnnotation.Url("http://senseis.xmp.net/?Seki", linkStyles), link.item)
  }

  @Test
  fun whitespaceCollapsesLikeAndroidAndOnlyBrBreaksLines() {
    assertEquals("one two three", convert("  one\n\ntwo   \n three").text)
    assertEquals("one\ntwo", convert("one<br> two<br/>").text.trimEnd())
  }

  @Test
  fun fontColoursUseAndroidsHtmlColourNames() {
    val result = convert("<font color='green'>g</font><font color=red>r</font><font color=\"#123456\">h</font>")

    assertEquals(listOf(SpanStyle(color = Color(0xFF00FF00))), result.styleAt("g"))
    assertEquals(listOf(SpanStyle(color = Color(0xFFFF0000))), result.styleAt("r"))
    assertEquals(listOf(SpanStyle(color = Color(0xFF123456))), result.styleAt("h"))
    assertTrue(convert("<font color='nonsense'>x</font>").spanStyles.isEmpty())
  }

  @Test
  fun anUnclosedTagRunsToTheEnd() {
    val result = convert("**<font color=red>(This puzzle is flawed.")

    assertEquals("**(This puzzle is flawed.", result.text)
    assertEquals(listOf(SpanStyle(color = Color(0xFFFF0000))), result.styleAt("(This puzzle is flawed."))
  }

  @Test
  fun imagesAreDroppedWithoutLeavingACharacter() {
    assertEquals(
      "consider clicking . Thanks",
      convert("""consider clicking <img src="http://i.imgur.com/83izWQF.png">. Thanks""").text,
    )
  }

  @Test
  fun textThatOnlyLooksLikeMarkupIsKept() {
    assertEquals("**-> Black wins & 3 < 4 &amp;", convert("**-> Black wins &amp; 3 &lt; 4 &amp;amp;").text)
  }
}
