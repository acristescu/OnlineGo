package io.zenandroid.onlinego.data.model.local

data class Time(
        val thinking_time: Double,
        val skip_bonus: Boolean? = null,
        val block_time: Double? = null,
        val periods: Long? = null,
        val period_time: Double? = null,
        val moves_left: Long? = null
) {
    companion object {
        fun fromMap(map:Map<*, *>): Time {
            return Time(
                    map["thinking_time"] as Double,
                    map["skip_bonus"] as Boolean?,
                    map["block_time"] as Double?,
                    (map["periods"] as Double?)?.toLong(),
                    map["period_time"] as Double?,
                    (map["moves_left"] as Double?)?.toLong()
            )
        }
    }
}