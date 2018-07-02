package io.zenandroid.onlinego.statuschips

import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder

class ChipAdapter : GroupAdapter<ViewHolder>() {
    init {
        setOnItemClickListener { item, _ ->
            (item as? Chip)?.onClick?.invoke()
        }
    }
}
