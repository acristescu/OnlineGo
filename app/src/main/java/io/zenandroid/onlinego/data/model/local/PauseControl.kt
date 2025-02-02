package io.zenandroid.onlinego.data.model.local

data class PauseControl (
    val vacationWhite: Boolean? = null,
    val vacationBlack: Boolean? = null,
    val weekend: Boolean? = null,
    val stoneRemoval: Boolean? = null,
    val server: Boolean? = null,
    val moderator: Boolean? = null,
    val pausedByThirdParty: Boolean? = null
)

fun PauseControl?.isPaused() =
    this?.let {
        this.vacationBlack == true
                || this.vacationWhite == true
                || this.weekend == true
                || this.stoneRemoval == true
                || this.server == true
                || this.moderator == true
                || this.pausedByThirdParty == true
    } ?: false

fun PauseControl?.isPlayerPaused() =
    when {
        this?.moderator == true -> true
        this?.pausedByThirdParty == true -> true
        else -> false
    }
