package io.zenandroid.onlinego.ui.screens.mygames

import android.view.View
import androidx.annotation.DrawableRes
import android.view.View.GONE
import android.view.View.VISIBLE
import com.xwray.groupie.viewbinding.BindableItem
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.databinding.ItemNewGameBinding

sealed class NewGameItem(
        var text: String,
        @DrawableRes val icon: Int? = null
) : BindableItem<ItemNewGameBinding>(text.hashCode().toLong()) {

    object AutoMatch : NewGameItem("Auto-match", R.drawable.ic_person_filled)
    object Custom : NewGameItem("Custom", R.drawable.ic_challenge)
    object LocalAI : NewGameItem("Offline AI", R.drawable.ic_robot)

    override fun bind(binding: ItemNewGameBinding, position: Int) {
        binding.text.text = text
        icon?.let {
            binding.icon.visibility = VISIBLE
            binding.icon.setImageResource(it)
        } ?: run { binding.icon.visibility = GONE }

    }

    override fun getLayout() = R.layout.item_new_game

    override fun equals(other: Any?): Boolean {
        return other is NewGameItem && other.text == text && other.icon == icon
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + (icon ?: 0)
        return result
    }

    override fun initializeViewBinding(view: View): ItemNewGameBinding = ItemNewGameBinding.bind(view)
}