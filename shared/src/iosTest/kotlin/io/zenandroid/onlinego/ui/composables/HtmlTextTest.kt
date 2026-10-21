package io.zenandroid.onlinego.ui.composables

import androidx.compose.ui.text.TextLinkStyles
import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlTextTest {

  @Test
  fun keepsTheTextOfADescriptionAndDropsItsTags() {
    val text = htmlToAnnotatedString(
      "<p>Black to play &amp; live</p><a href=\"https://online-go.com\">More</a><br/>&lt;end&gt;",
      TextLinkStyles(),
    ).text
    assertEquals("Black to play & live\nMore\n<end>", text)
  }
}
