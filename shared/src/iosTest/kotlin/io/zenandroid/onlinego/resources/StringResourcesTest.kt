package io.zenandroid.onlinego.resources

import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StringResourcesTest {

  @Test
  fun apostrophesAndQuotesAreLiteral() = runTest {
    assertEquals(
      "If it were white's turn, it could \"escape\" by connecting another stone.",
      getString(Res.string.tutorial_basics_thebasics_capture1_page4_text),
    )
    assertEquals(
      "KO - the \"no repetition\" rule",
      getString(Res.string.tutorial_intermediate_ko_title)
    )
  }

  @Test
  fun escapedNewlinesBecomeLineBreaks() = runTest {
    assertEquals("Play\nOnline", getString(Res.string.button_play_online))
  }

  @Test
  fun aSinglePercentSignSurvivesFormatting() = runTest {
    assertEquals("42%", getString(Res.string.game_player_details_percentage, 42))
  }

  @Test
  fun pluralsPickTheEnglishForm() = runTest {
    assertEquals(
      "It's your turn in 1 game.",
      getPluralString(Res.plurals.mygames_your_turn_count, 1, 1)
    )
    assertEquals(
      "It's your turn in 3 games.",
      getPluralString(Res.plurals.mygames_your_turn_count, 3, 3)
    )
  }

  @Test
  fun stringArraysKeepTheirItems() = runTest {
    val items = getStringArray(Res.array.whats_new_changelog)
    assertTrue(items.first().startsWith("✨ New localization system!"))
    assertTrue(items.last().contains("GoPopz"))
  }

  @Test
  fun tutorialKeysResolveByName() = runTest {
    assertEquals("Basics", getString(Res.allStringResources.getValue("tutorial_basics_title")))
  }
}
