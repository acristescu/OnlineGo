package io.zenandroid.onlinego.ui.screens.mygames

import androidx.compose.runtime.Composable
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.resources.Res
import io.zenandroid.onlinego.resources.mygames_outcome_black_won
import io.zenandroid.onlinego.resources.mygames_outcome_black_won_with_reason
import io.zenandroid.onlinego.resources.mygames_outcome_cancelled
import io.zenandroid.onlinego.resources.mygames_outcome_lost
import io.zenandroid.onlinego.resources.mygames_outcome_lost_with_reason
import io.zenandroid.onlinego.resources.mygames_outcome_reason_disconnection
import io.zenandroid.onlinego.resources.mygames_outcome_reason_other
import io.zenandroid.onlinego.resources.mygames_outcome_reason_points
import io.zenandroid.onlinego.resources.mygames_outcome_reason_resignation
import io.zenandroid.onlinego.resources.mygames_outcome_reason_timeout
import io.zenandroid.onlinego.resources.mygames_outcome_white_won
import io.zenandroid.onlinego.resources.mygames_outcome_white_won_with_reason
import io.zenandroid.onlinego.resources.mygames_outcome_won
import io.zenandroid.onlinego.resources.mygames_outcome_won_with_reason
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private enum class OutcomePerspective {
  USER_LOST,
  USER_WON,
  BLACK_WON,
  WHITE_WON
}

private val POINTS_REGEX =
  Regex("""^([+-]?[0-9]+(?:[.,][0-9]+)?)\s*points?$""", RegexOption.IGNORE_CASE)

fun formatGameOutcome(
  game: Game,
  userId: Long?,
  getString: (StringResource, Array<out Any>) -> String,
): String {
  if (game.annulled == true || game.outcome?.trim().equals("Cancellation", ignoreCase = true)) {
    return getString(Res.string.mygames_outcome_cancelled, emptyArray())
  }

  val perspective = when {
    userId == game.blackPlayer.id ->
      if (game.blackLost == true) OutcomePerspective.USER_LOST
      else OutcomePerspective.USER_WON

    userId == game.whitePlayer.id ->
      if (game.whiteLost == true) OutcomePerspective.USER_LOST
      else OutcomePerspective.USER_WON

    game.whiteLost == true ->
      OutcomePerspective.BLACK_WON

    else ->
      OutcomePerspective.WHITE_WON
  }

  val outcome = game.outcome?.trim().orEmpty()
  val pointsMatch = POINTS_REGEX.find(outcome)

  val reason = when {
    outcome.equals("Resignation", ignoreCase = true) ->
      getString(Res.string.mygames_outcome_reason_resignation, emptyArray())

    outcome.equals("Timeout", ignoreCase = true) ->
      getString(Res.string.mygames_outcome_reason_timeout, emptyArray())

    outcome.equals("Disconnection", ignoreCase = true) ->
      getString(Res.string.mygames_outcome_reason_disconnection, emptyArray())

    pointsMatch != null ->
      getString(Res.string.mygames_outcome_reason_points, arrayOf(pointsMatch.groupValues[1]))

    outcome.isNotBlank() ->
      getString(Res.string.mygames_outcome_reason_other, arrayOf(outcome))

    else -> null
  }

  return if (reason != null) {
    when (perspective) {
      OutcomePerspective.USER_LOST -> getString(
        Res.string.mygames_outcome_lost_with_reason,
        arrayOf(reason)
      )

      OutcomePerspective.USER_WON -> getString(
        Res.string.mygames_outcome_won_with_reason,
        arrayOf(reason)
      )

      OutcomePerspective.BLACK_WON -> getString(
        Res.string.mygames_outcome_black_won_with_reason,
        arrayOf(reason)
      )

      OutcomePerspective.WHITE_WON -> getString(
        Res.string.mygames_outcome_white_won_with_reason,
        arrayOf(reason)
      )
    }
  } else {
    when (perspective) {
      OutcomePerspective.USER_LOST -> getString(Res.string.mygames_outcome_lost, emptyArray())
      OutcomePerspective.USER_WON -> getString(Res.string.mygames_outcome_won, emptyArray())
      OutcomePerspective.BLACK_WON -> getString(Res.string.mygames_outcome_black_won, emptyArray())
      OutcomePerspective.WHITE_WON -> getString(Res.string.mygames_outcome_white_won, emptyArray())
    }
  }
}

@Composable
fun formatGameOutcome(game: Game, userId: Long?): String {
  val perspective = when {
    game.annulled == true || game.outcome?.trim().equals("Cancellation", ignoreCase = true) ->
      return stringResource(Res.string.mygames_outcome_cancelled)

    userId == game.blackPlayer.id ->
      if (game.blackLost == true) OutcomePerspective.USER_LOST
      else OutcomePerspective.USER_WON

    userId == game.whitePlayer.id ->
      if (game.whiteLost == true) OutcomePerspective.USER_LOST
      else OutcomePerspective.USER_WON

    game.whiteLost == true ->
      OutcomePerspective.BLACK_WON

    else ->
      OutcomePerspective.WHITE_WON
  }

  val outcome = game.outcome?.trim().orEmpty()
  val pointsMatch = POINTS_REGEX.find(outcome)

  val reason = when {
    outcome.equals("Resignation", ignoreCase = true) ->
      stringResource(Res.string.mygames_outcome_reason_resignation)

    outcome.equals("Timeout", ignoreCase = true) ->
      stringResource(Res.string.mygames_outcome_reason_timeout)

    outcome.equals("Disconnection", ignoreCase = true) ->
      stringResource(Res.string.mygames_outcome_reason_disconnection)

    pointsMatch != null ->
      stringResource(Res.string.mygames_outcome_reason_points, pointsMatch.groupValues[1])

    outcome.isNotBlank() ->
      stringResource(Res.string.mygames_outcome_reason_other, outcome)

    else -> null
  }

  return if (reason != null) {
    when (perspective) {
      OutcomePerspective.USER_LOST -> stringResource(
        Res.string.mygames_outcome_lost_with_reason,
        reason
      )

      OutcomePerspective.USER_WON -> stringResource(
        Res.string.mygames_outcome_won_with_reason,
        reason
      )

      OutcomePerspective.BLACK_WON -> stringResource(
        Res.string.mygames_outcome_black_won_with_reason,
        reason
      )

      OutcomePerspective.WHITE_WON -> stringResource(
        Res.string.mygames_outcome_white_won_with_reason,
        reason
      )
    }
  } else {
    when (perspective) {
      OutcomePerspective.USER_LOST -> stringResource(Res.string.mygames_outcome_lost)
      OutcomePerspective.USER_WON -> stringResource(Res.string.mygames_outcome_won)
      OutcomePerspective.BLACK_WON -> stringResource(Res.string.mygames_outcome_black_won)
      OutcomePerspective.WHITE_WON -> stringResource(Res.string.mygames_outcome_white_won)
    }
  }
}
