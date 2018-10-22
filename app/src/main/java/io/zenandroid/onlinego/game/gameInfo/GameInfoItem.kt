package io.zenandroid.onlinego.game.gameInfo

import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.zenandroid.onlinego.R
import kotlinx.android.synthetic.main.item_game_info.*

class GameInfoItem(
        val title: String,
        val value: String
): Item() {
    override fun getLayout() = R.layout.item_game_info

    override fun bind(holder: ViewHolder, position: Int) {
        holder.title.text = title
        holder.value.text = value
    }
}
