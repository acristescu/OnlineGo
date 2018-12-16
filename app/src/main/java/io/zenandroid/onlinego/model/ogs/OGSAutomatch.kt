package io.zenandroid.onlinego.model.ogs

data class OGSAutomatch(
        val uuid: String,
        val size_speed_options: List<SizeSpeedOption>
)

data class SizeSpeedOption(
        val size: String,
        val speed: String
)