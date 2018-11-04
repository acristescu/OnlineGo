package io.zenandroid.onlinego.mygames

import android.support.annotation.DrawableRes
import android.view.View.GONE
import android.view.View.VISIBLE
import com.xwray.groupie.kotlinandroidextensions.Item
import io.zenandroid.onlinego.R
import kotlinx.android.synthetic.main.item_new_game.*

open class NewGameItem(
        var text: String,
        @DrawableRes val icon: Int? = null,
        val onClick: (() -> Unit)? = null
) : Item(text.hashCode().toLong()) {
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
