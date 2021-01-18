package io.zenandroid.onlinego.ui.screens.game.gameInfo

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.databinding.ItemGameInfoBinding

class GameInfoItem(
        val title: String,
        val value: String
): BindableItem<ItemGameInfoBinding>() {
    override fun getLayout() = R.layout.item_game_info

    override fun bind(binding: ItemGameInfoBinding, position: Int) {
        binding.title.text = title
        binding.value.text = value
    }

    override fun initializeViewBinding(view: View): ItemGameInfoBinding = ItemGameInfoBinding.bind(view)
}
