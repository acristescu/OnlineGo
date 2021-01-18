package io.zenandroid.onlinego.ui.items

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.databinding.ItemLoadingGameBinding

class LoadingGameItemCard : BindableItem<ItemLoadingGameBinding>() {
    override fun bind(binding: ItemLoadingGameBinding, position: Int) {

    }

    override fun getLayout() = R.layout.item_loading_game
    override fun initializeViewBinding(view: View): ItemLoadingGameBinding = ItemLoadingGameBinding.bind(view)
}