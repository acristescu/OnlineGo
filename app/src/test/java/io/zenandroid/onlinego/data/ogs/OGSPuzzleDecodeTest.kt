package io.zenandroid.onlinego.data.ogs

import io.zenandroid.onlinego.data.model.ogs.OGSPuzzle
import io.zenandroid.onlinego.utils.appJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A real `GET /api/v1/puzzles/{id}` body, kept verbatim. It is the payload that broke the puzzle
 * directory on the kotlinx.serialization switch (`"order": 99999999.0` into an `Int?`), and it
 * also happens to cover the recursive move_tree, ISO instants and quoted numbers in one go.
 */
class OGSPuzzleDecodeTest {

  private val body =
    """{"id":2625,"order":99999999.0,"owner":{"id":64817,"username":"mark5000","country":"us","icon":"https://user-uploads.online-go.com/9b5320a244a5d988372d7e6e830a8fbe-64.png","ratings":{"version":5,"overall":{"rating":2254.707619884256,"deviation":84.48105170755471,"volatility":0.06047579984841494}},"ranking":33.73828501242196,"professional":false,"ui_class":"supporter moderator"},"name":"Exercise 001","created":"2015-06-18T03:51:23.090817Z","modified":"2026-09-29T12:55:57.166890Z","puzzle":{"mode":"puzzle","name":"Exercise 001","puzzle_type":"life_and_death","width":19,"height":19,"initial_state":{"white":"arbrcrdreres","black":"aqbpdpeqfqfrhrbs"},"puzzle_opponent_move_mode":"automatic","puzzle_player_move_mode":"free","puzzle_rank":"5","puzzle_description":"Learn and exercise basic concepts in life and death!","puzzle_collection":"242","initial_player":"black","move_tree":{"x":-1,"y":-1,"text":"Black to kill.","branches":[{"x":2,"y":18,"correct_answer":true},{"x":0,"y":18,"branches":[{"x":2,"y":18,"wrong_answer":true}]},{"x":3,"y":18,"branches":[{"x":2,"y":18,"wrong_answer":true}]}]}},"private":false,"width":19,"height":19,"type":"life_and_death","has_solution":false,"rating":4.676794133053955,"rating_count":3818,"rank":5,"collection":{"id":242,"owner":{"id":64817,"username":"mark5000","country":"us","icon":"https://user-uploads.online-go.com/9b5320a244a5d988372d7e6e830a8fbe-64.png","ratings":{"version":5,"overall":{"rating":2254.707619884256,"deviation":84.48105170755471,"volatility":0.06047579984841494}},"ranking":33.73828501242196,"professional":false,"ui_class":"supporter moderator"},"name":"Exercises for Beginners","created":"2015-06-18T03:50:00.350378Z","private":false,"price":"0.00","starting_puzzle":{"id":2625,"initial_state":{"white":"arbrcrdreres","black":"aqbpdpeqfqfrhrbs"},"width":19,"height":19},"rating":4.5836838711658325,"rating_count":32569,"puzzle_count":107,"min_rank":5,"max_rank":21,"view_count":11309882,"solved_count":5112722,"attempt_count":11462633,"color_transform_enabled":true,"position_transform_enabled":true},"view_count":736739,"solved_count":230990,"attempt_count":595931}"""

  @Test
  fun `a real puzzle response decodes`() {
    val puzzle = appJson.decodeFromString<OGSPuzzle>(body)

    assertEquals(2625L, puzzle.id)
    assertEquals(99999999, puzzle.order)
    assertEquals("Exercise 001", puzzle.name)
    assertNotNull(puzzle.created)
    assertEquals(5, puzzle.rank)
  }

  @Test
  fun `the recursive move tree and its answer flags survive`() {
    val tree = appJson.decodeFromString<OGSPuzzle>(body).puzzle.move_tree

    assertNotNull(tree)
    assertEquals("Black to kill.", tree!!.text)
    assertEquals(3, tree.branches!!.size)
    assertTrue(tree.branches!!.first().correct_answer == true)
    assertTrue(tree.branches!![1].branches!!.first().wrong_answer == true)
  }

  @Test
  fun `the nested collection decodes too`() {
    val collection = appJson.decodeFromString<OGSPuzzle>(body).collection

    assertNotNull(collection)
    assertEquals(242L, collection!!.id)
    assertEquals(107, collection.puzzle_count)
  }
}
