package io.zenandroid.onlinego.ui.screens.joseki

import kotlin.test.Test
import kotlin.test.assertEquals

class JosekiLinksTest {

  @Test
  fun positionLinksLoadTheirNode() {
    assertEquals(JosekiLink.Position(123), josekiLinkOf("Position:123"))
    assertEquals(JosekiLink.Position(123), josekiLinkOf("123"))
    assertEquals(JosekiLink.Position(123), josekiLinkOf("https://online-go.com/joseki/123"))
    assertEquals(JosekiLink.Position(123), josekiLinkOf("https://beta.online-go.com/joseki/123/"))
  }

  @Test
  fun unreadablePositionLinksHaveNoNode() {
    assertEquals(JosekiLink.Position(null), josekiLinkOf("Position:abc"))
    assertEquals(JosekiLink.Position(null), josekiLinkOf("https://online-go.com/joseki/"))
  }

  @Test
  fun youtubeLinksAreVideos() {
    assertEquals(JosekiLink.Video, josekiLinkOf("https://youtu.be/uGd_LM3A1H4?si=SL-EB3ZZXxuwC85m"))
    assertEquals(JosekiLink.Video, josekiLinkOf("https://www.youtube.com/watch?v=abc"))
    assertEquals(JosekiLink.Video, josekiLinkOf("https://youtube.com/watch?v=abc"))
  }

  @Test
  fun otherLinksAreExternal() {
    assertEquals(JosekiLink.External, josekiLinkOf("http://ps.waltheri.net/654520"))
    assertEquals(JosekiLink.External, josekiLinkOf("https://online-go.com/game/1"))
    assertEquals(JosekiLink.External, josekiLinkOf("https://m.youtube.com/watch?v=abc"))
  }

  @Test
  fun videoAndExternalLinksGetTheirLabelInsideTheLinkText() {
    val markdown = "Watch [this](https://youtu.be/x), see [games](http://ps.waltheri.net/1) " +
        "or [Position 2](2) and [there](Position:3)."

    assertEquals(
      "Watch [this (video)](https://youtu.be/x), see [games (external)](http://ps.waltheri.net/1) " +
          "or [Position 2](2) and [there](Position:3).",
      labelJosekiLinks(markdown, "(video)", "(external)"),
    )
  }

  @Test
  fun labelsKeepTheLinkFormatting() {
    assertEquals(
      "# Title\n\n* [**bold** (external)](<http://a.b/c d>)",
      labelJosekiLinks("# Title\n\n* [**bold**](<http://a.b/c d>)", "(video)", "(external)"),
    )
  }

  @Test
  fun textWithoutLinksIsUnchanged() {
    val markdown = "## Empty Goban\n\nInfinite possibilities await! [not a link] <b>"

    assertEquals(markdown, labelJosekiLinks(markdown, "(video)", "(external)"))
  }
}
