package io.zenandroid.onlinego.model.ogs

data class OGSChallenge(
        val id: Long,
        val challenger: OGSPlayer?,
        val challenged: OGSPlayer?,
        val game: OGSGame?
)