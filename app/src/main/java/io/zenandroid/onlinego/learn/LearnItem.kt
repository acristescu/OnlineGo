package io.zenandroid.onlinego.learn

import androidx.annotation.DrawableRes
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.showIf
import kotlinx.android.synthetic.main.item_learn.*

class LearnItem(
        private val title: String,
        private val subtitle: String,
        @DrawableRes private val icon: Int,
        private val ctaText: String,
        private val badgeVisibleCallback: () -> Boolean
) : Item() {
    override fun bind(holder: GroupieViewHolder, position: Int) {
        holder.apply {
            titleView.text = title
            subtitleView.text = subtitle
            iconView.setImageResource(icon)
            ctaView.text = ctaText
            badge.showIf(badgeVisibleCallback.invoke())
        }
    }

    override fun getLayout(): Int = R.layout.item_learn
}