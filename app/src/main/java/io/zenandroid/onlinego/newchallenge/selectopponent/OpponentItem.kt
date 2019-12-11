package io.zenandroid.onlinego.newchallenge.selectopponent

import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.model.ogs.OGSPlayer
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import io.zenandroid.onlinego.utils.processGravatarURL
import kotlinx.android.synthetic.main.item_opponent.*

class OpponentItem(
        val opponent: OGSPlayer
) : Item(opponent.id) {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.nameView.text = opponent.username
        viewHolder.extraInfoView.text = formatRank(egfToRank(opponent.ratings?.overall?.rating))
        opponent.icon?.let {
            Glide.with(viewHolder.iconView)
                    .load(processGravatarURL(it, viewHolder.iconView.width))
                    .transition(DrawableTransitionOptions.withCrossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()))
                    .apply(RequestOptions().centerCrop().placeholder(R.drawable.ic_person_outline))
                    .apply(RequestOptions().circleCrop().diskCacheStrategy(DiskCacheStrategy.RESOURCE))
                    .into(viewHolder.iconView)
                    .waitForLayout()
        }
    }

    override fun getLayout() = R.layout.item_opponent

}