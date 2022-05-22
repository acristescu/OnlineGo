package io.zenandroid.onlinego.utils

import io.zenandroid.onlinego.data.model.local.Clock
import io.zenandroid.onlinego.data.model.local.Time
import io.zenandroid.onlinego.data.repositories.ClockDriftRepository
import org.koin.core.context.GlobalContext

private val clockDriftRepository: ClockDriftRepository by GlobalContext.get().inject()

fun computeTimeLeft(
    clock: Clock,
    playerTimeSimple: Long?,
    playerTime: Time?,
    currentPlayer: Boolean,
    pausedSince: Long?
) = computeTimeLeft(clockDriftRepository.serverTime, clock, playerTimeSimple, playerTime, currentPlayer, pausedSince)
