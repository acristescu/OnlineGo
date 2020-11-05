package io.zenandroid.onlinego.ui.items.statuschips

import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder

class ChipAdapter : GroupAdapter<GroupieViewHolder>() {
    init {
        setOnItemClickListener { item, _ ->
            (item as? Chip)?.onClick?.invoke()
        }
    }
}
