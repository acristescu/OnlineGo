package io.zenandroid.onlinego.ui.screens.game.gameInfo

import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import io.zenandroid.onlinego.R
import kotlinx.android.synthetic.main.item_game_info.*

class GameInfoItem(
        val title: String,
        val value: String
): Item() {
    override fun getLayout() = R.layout.item_game_info

    override fun bind(holder: GroupieViewHolder, position: Int) {
        holder.title.text = title
        holder.value.text = value
    }
}
