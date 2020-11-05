package io.zenandroid.onlinego.data.model.ogs

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import io.zenandroid.onlinego.data.model.Position

@Entity
data class JosekiPosition (
        var description: String? = null,
        var variation_label: String? = null,
        var category: PlayCategory? = null,
        var joseki_source_id: String? = null,
        var marks: String? = null,
        @Ignore
        val tags: List<Any>? = null,
        var placement: String? = null,
        var play: String? = null,
        var contributor: Long? = null,
        @PrimaryKey
        var node_id: Long? = null,
        var comment_count: Long? = null,
        @Ignore
        var next_moves: List<JosekiPosition>? = null,
        @Ignore
        val parent: JosekiPosition? = null,
        var parent_id: Long? = null,
        var child_count: Long? = null,
        var topicId: Long? = null,
        var db_locked_down: Boolean? = null,
        @Ignore
        val joseki_source: Any? = null,
        @Transient
        @Ignore
        var labels: List<Position.Mark>? = null
)
enum class PlayCategory {
    IDEAL, GOOD, MISTAKE, TRICK, QUESTION, LABEL
}