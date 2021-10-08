package io.zenandroid.onlinego.utils

import io.zenandroid.onlinego.data.model.local.Clock
import io.zenandroid.onlinego.data.model.local.Time
import io.zenandroid.onlinego.data.repositories.ClockDriftRepository
import io.zenandroid.onlinego.ui.screens.game.GamePresenter
import org.koin.core.context.GlobalContext

private val clockDriftRepository: ClockDriftRepository by GlobalContext.get().inject()

fun computeTimeLeft(
    clock: Clock,
    playerTimeSimple: Long?,
    playerTime: Time?,
    currentPlayer: Boolean,
    pausedSince: Long?
): GamePresenter.TimerDetails {
    val now = clockDriftRepository.serverTime.coerceAtMost(pausedSince ?: Long.MAX_VALUE)
    val baseTime = clock.lastMove.coerceAtMost(pausedSince ?: Long.MAX_VALUE)
    var timeLeft = 0L
    var secondLine: String? = null

    if(playerTimeSimple != null) {
        // Simple timer
        timeLeft = if(playerTimeSimple == 0L) 0 else {
            playerTimeSimple - if (currentPlayer) now else baseTime
        }
    } else if (playerTime != null) {
        timeLeft = if(currentPlayer) {
            baseTime + (playerTime.thinking_time * 1000).toLong() - now
        } else {
            (playerTime.thinking_time * 1000).toLong()
        }

        if(playerTime.moves_left != null) {

            // Canadian timer
            if(timeLeft < 0 || playerTime.thinking_time == 0.0) {
                timeLeft = baseTime + ((playerTime.thinking_time + playerTime.block_time!!) * 1000).toLong() - if(currentPlayer) now else baseTime
            }
            secondLine = "+${formatMillis((playerTime.block_time!! * 1000).toLong())} / ${playerTime.moves_left}"
        } else if(playerTime.periods != null) {

            // Byo Yomi timer
            var periodsLeft = playerTime.periods
            if(timeLeft < 0 || playerTime.thinking_time == 0.0) {
                val periodOffset = Math.ceil((-timeLeft / 1000.0) / playerTime.period_time!!).coerceAtLeast(0.0)

                while(timeLeft < 0) {
                    timeLeft += (playerTime.period_time * 1000).toLong()
                }

                periodsLeft = playerTime.periods - periodOffset.toLong()
                if(periodsLeft < 0) {
                    timeLeft = 0
                }
            }
            if(!currentPlayer && timeLeft == 0L) {
                timeLeft = (playerTime.period_time!! * 1000).toLong()
            }
            secondLine = "$periodsLeft x ${formatMillis((playerTime.period_time!! * 1000).toLong())}"
        } else {
            // absolute timer or fischer timer, nothing to do
        }
    } else {
        // No timer
        return GamePresenter.TimerDetails(
            expired = false,
            firstLine = "∞",
            secondLine = null,
            timeLeft = Long.MAX_VALUE
        )
    }

    return GamePresenter.TimerDetails(
        expired = timeLeft <= 0,
        firstLine = formatMillis(timeLeft),
        secondLine = secondLine,
        timeLeft = timeLeft
    )
}
