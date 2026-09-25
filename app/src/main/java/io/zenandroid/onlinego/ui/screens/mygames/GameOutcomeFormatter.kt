package io.zenandroid.onlinego.ui.screens.mygames

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.Game

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
  getString: (Int, Array<out Any>) -> String,
): String {
  if (game.annulled == true || game.outcome?.trim().equals("Cancellation", ignoreCase = true)) {
    return getString(R.string.mygames_outcome_cancelled, emptyArray())
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
      getString(R.string.mygames_outcome_reason_resignation, emptyArray())

    outcome.equals("Timeout", ignoreCase = true) ->
      getString(R.string.mygames_outcome_reason_timeout, emptyArray())

    outcome.equals("Disconnection", ignoreCase = true) ->
      getString(R.string.mygames_outcome_reason_disconnection, emptyArray())

    pointsMatch != null ->
      getString(R.string.mygames_outcome_reason_points, arrayOf(pointsMatch.groupValues[1]))

    outcome.isNotBlank() ->
      getString(R.string.mygames_outcome_reason_other, arrayOf(outcome))

    else -> null
  }

  return if (reason != null) {
    when (perspective) {
      OutcomePerspective.USER_LOST -> getString(
        R.string.mygames_outcome_lost_with_reason,
        arrayOf(reason)
      )

      OutcomePerspective.USER_WON -> getString(
        R.string.mygames_outcome_won_with_reason,
        arrayOf(reason)
      )

      OutcomePerspective.BLACK_WON -> getString(
        R.string.mygames_outcome_black_won_with_reason,
        arrayOf(reason)
      )

      OutcomePerspective.WHITE_WON -> getString(
        R.string.mygames_outcome_white_won_with_reason,
        arrayOf(reason)
      )
    }
  } else {
    when (perspective) {
      OutcomePerspective.USER_LOST -> getString(R.string.mygames_outcome_lost, emptyArray())
      OutcomePerspective.USER_WON -> getString(R.string.mygames_outcome_won, emptyArray())
      OutcomePerspective.BLACK_WON -> getString(R.string.mygames_outcome_black_won, emptyArray())
      OutcomePerspective.WHITE_WON -> getString(R.string.mygames_outcome_white_won, emptyArray())
    }
  }
}

fun formatGameOutcome(context: Context, game: Game, userId: Long?): String =
  formatGameOutcome(game, userId) { resId, args ->
    if (args.isEmpty()) context.getString(resId)
    else context.getString(resId, *args)
  }

@Composable
fun formatGameOutcome(game: Game, userId: Long?): String {
  val perspective = when {
    game.annulled == true || game.outcome?.trim().equals("Cancellation", ignoreCase = true) ->
      return stringResource(R.string.mygames_outcome_cancelled)

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
      stringResource(R.string.mygames_outcome_reason_resignation)

    outcome.equals("Timeout", ignoreCase = true) ->
      stringResource(R.string.mygames_outcome_reason_timeout)

    outcome.equals("Disconnection", ignoreCase = true) ->
      stringResource(R.string.mygames_outcome_reason_disconnection)

    pointsMatch != null ->
      stringResource(R.string.mygames_outcome_reason_points, pointsMatch.groupValues[1])

    outcome.isNotBlank() ->
      stringResource(R.string.mygames_outcome_reason_other, outcome)

    else -> null
  }

  return if (reason != null) {
    when (perspective) {
      OutcomePerspective.USER_LOST -> stringResource(
        R.string.mygames_outcome_lost_with_reason,
        reason
      )

      OutcomePerspective.USER_WON -> stringResource(
        R.string.mygames_outcome_won_with_reason,
        reason
      )

      OutcomePerspective.BLACK_WON -> stringResource(
        R.string.mygames_outcome_black_won_with_reason,
        reason
      )

      OutcomePerspective.WHITE_WON -> stringResource(
        R.string.mygames_outcome_white_won_with_reason,
        reason
      )
    }
  } else {
    when (perspective) {
      OutcomePerspective.USER_LOST -> stringResource(R.string.mygames_outcome_lost)
      OutcomePerspective.USER_WON -> stringResource(R.string.mygames_outcome_won)
      OutcomePerspective.BLACK_WON -> stringResource(R.string.mygames_outcome_black_won)
      OutcomePerspective.WHITE_WON -> stringResource(R.string.mygames_outcome_white_won)
    }
  }
}
