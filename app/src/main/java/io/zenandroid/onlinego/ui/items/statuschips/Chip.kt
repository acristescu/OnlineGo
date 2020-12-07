package io.zenandroid.onlinego.ui.items.statuschips

import android.content.res.ColorStateList
import android.os.Build
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import android.view.View.GONE
import android.view.View.VISIBLE
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.R
import kotlinx.android.synthetic.main.view_chip.*

open class Chip(
        var text: String,
        @DrawableRes val icon: Int? = null,
        @ColorRes val bgColor: Int = R.color.white,
        @ColorRes val fgColor: Int = R.color.headerPrimary,
        val onClick: (() -> Unit)? = null
) : Item(text.hashCode().toLong()) {
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.text.text = text
        viewHolder.card.setCardBackgroundColor(ContextCompat.getColor(viewHolder.icon.context, bgColor))
        viewHolder.text.setTextColor(ContextCompat.getColor(viewHolder.icon.context, fgColor))
        icon?.let {
            viewHolder.icon.visibility = VISIBLE
            viewHolder.icon.setImageResource(it)
            viewHolder.icon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(viewHolder.icon.context, fgColor))
        } ?: run { viewHolder.icon.visibility = GONE }

    }

    override fun getLayout() = R.layout.view_chip

    override fun equals(other: Any?): Boolean {
        return other is Chip && other.text == text && other.icon == icon
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + (icon ?: 0)
        return result
    }
}

class FinishedChip(onClick: (() -> Unit)? = null) : Chip("Finished", R.drawable.ic_question_mark, onClick = onClick)
class PlayingChip(onClick: (() -> Unit)? = null) : Chip("Playing", R.drawable.ic_question_mark, onClick = onClick)
class StoneRemovalChip(onClick: (() -> Unit)? = null) : Chip("Scoring", R.drawable.ic_question_mark, onClick = onClick)
class PassedChip(onClick: (() -> Unit)? = null) : Chip("Player Passed", R.drawable.ic_question_mark, onClick = onClick)
class AnalysisChip(onClick: (() -> Unit)? = null) : Chip("Analysis", R.drawable.ic_question_mark, onClick = onClick)
class EstimationChip(onClick: (() -> Unit)? = null) : Chip("Estimation", R.drawable.ic_question_mark, onClick = onClick)