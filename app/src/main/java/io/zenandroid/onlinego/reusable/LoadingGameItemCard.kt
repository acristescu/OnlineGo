package io.zenandroid.onlinego.reusable

import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import io.zenandroid.onlinego.R

class LoadingGameItemCard : Item() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {

    }

    override fun getLayout() = R.layout.item_loading_game
}