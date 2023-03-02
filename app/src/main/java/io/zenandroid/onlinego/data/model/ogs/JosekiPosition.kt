package io.zenandroid.onlinego.data.model.ogs

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import io.zenandroid.onlinego.data.model.Mark

@Entity(indices = [Index(value = ["play"], unique = true)])
@Immutable
data class JosekiPosition(
    var description: String? = null,
    var variation_label: String? = null,
    var category: PlayCategory? = null,
    var joseki_source_id: String? = null,
    var marks: String? = null,
    var placement: String? = null,
    var play: String? = null,
    var contributor: Long? = null,
    @PrimaryKey
    var node_id: Long? = null,
    var comment_count: Long? = null,
    var parent_id: Long? = null,
    var child_count: Long? = null,
    var topicId: Long? = null,
    var db_locked_down: Boolean? = null,
    @Ignore
    val tags: List<Any>? = null,
    @Ignore
    var next_moves: List<JosekiPosition>? = null,
    @Ignore
    val parent: JosekiPosition? = null,
    @Ignore
    val joseki_source: Any? = null,
    @Transient
    @Ignore
    var labels: List<Mark>? = null,
) {
    constructor(
        description: String? = null,
        variation_label: String? = null,
        category: PlayCategory? = null,
        joseki_source_id: String? = null,
        marks: String? = null,
        placement: String? = null,
        play: String? = null,
        contributor: Long? = null,
        node_id: Long? = null,
        comment_count: Long? = null,
        parent_id: Long? = null,
        child_count: Long? = null,
        topicId: Long? = null,
        db_locked_down: Boolean? = null,
    ) : this(
        description,
        variation_label,
        category,
        joseki_source_id,
        marks,
        placement,
        play,
        contributor,
        node_id,
        comment_count,
        parent_id,
        child_count,
        topicId,
        db_locked_down,
        null,
        null,
        null,
        null,
        null
    )

}


enum class PlayCategory {
    IDEAL, GOOD, MISTAKE, TRICK, QUESTION, LABEL
}