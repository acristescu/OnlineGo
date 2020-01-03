package io.zenandroid.onlinego.mygames

import androidx.annotation.DrawableRes
import android.view.View.GONE
import android.view.View.VISIBLE
import com.xwray.groupie.kotlinandroidextensions.Item
import io.zenandroid.onlinego.R
import kotlinx.android.synthetic.main.item_new_game.*

sealed class NewGameItem(
        var text: String,
        @DrawableRes val icon: Int? = null,
        val onClick: (() -> Unit)? = null
) : Item(text.hashCode().toLong()) {

    object AutoMatch : NewGameItem("Auto-match", R.drawable.ic_person_filled)
    object Custom : NewGameItem("Custom", R.drawable.ic_challenge)

    override fun bind(viewHolder: com.xwray.groupie.kotlinandroidextensions.ViewHolder, position: Int) {
        viewHolder.text.text = text
        icon?.let {
            viewHolder.icon.visibility = VISIBLE
            viewHolder.icon.setImageResource(it)
        } ?: run { viewHolder.icon.visibility = GONE }

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
}