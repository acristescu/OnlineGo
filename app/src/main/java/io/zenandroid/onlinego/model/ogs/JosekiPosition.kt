package io.zenandroid.onlinego.model.ogs

import io.zenandroid.onlinego.model.Position

data class JosekiPosition (
        val description: String?,
        val variation_label: String?,
        val category: PlayCategory?,
        val joseki_source_id: String?,
        val marks: String?,
        val tags: List<Any>?,
        val placement: String?,
        val play: String?,
        val contributor: Long?,
        val node_id: Long?,
        val comment_count: Long?,
        val next_moves: List<JosekiPosition>?,
        val parent: JosekiPosition?,
        val child_count: Long?,
        val topicId: Long?,
        val db_locked_down: Boolean?,
        val joseki_source: Any?,

        @Transient
        var labels: List<Position.Mark>? = null
)

enum class PlayCategory {
    IDEAL, GOOD, MISTAKE, TRICK, QUESTION, LABEL
}