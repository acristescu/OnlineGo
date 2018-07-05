package io.zenandroid.onlinego.learn

import android.support.annotation.DrawableRes
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.zenandroid.onlinego.R
import kotlinx.android.synthetic.main.item_learn.*

class LearnItem(
        val title: String,
        val subtitle: String,
        @DrawableRes val icon: Int,
        val ctaText: String
) : Item() {
    override fun bind(holder: ViewHolder, position: Int) {
        holder.titleView.text = title
        holder.subtitleView.text = subtitle
        holder.iconView.setImageResource(icon)
        holder.ctaView.text = ctaText
    }

    override fun getLayout(): Int = R.layout.item_learn
}