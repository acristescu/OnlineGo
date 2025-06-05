package io.zenandroid.onlinego.data.model.ogs

import androidx.room.Entity
import androidx.room.Ignore
import io.zenandroid.onlinego.data.model.Cell

@Entity
data class AnalysisMessage (
    var name: String? = null,
    var from: Int? = null,
    var moves: List<Cell>? = null,
    @Ignore var marks: Map<String, String>? = null,
    @Ignore var pen_marks: List<Any>? = null,
    @Ignore var branch_move: Long? = null, // deprecated
) {
    @Ignore val type: String = "analysis"
    
    constructor(name: String?, from: Int?, moves: List<Cell>?) : this() {
        this.name = name
        this.from = from
        this.moves = moves
    }
}
