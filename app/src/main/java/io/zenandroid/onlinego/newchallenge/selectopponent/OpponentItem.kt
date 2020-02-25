package io.zenandroid.onlinego.newchallenge.selectopponent

import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.DP
import io.zenandroid.onlinego.model.local.Player
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import io.zenandroid.onlinego.utils.processGravatarURL
import kotlinx.android.synthetic.main.item_opponent.*


class OpponentItem(
        val opponent: Player
) : Item(opponent.id) {
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.nameView.text = opponent.username
        viewHolder.extraInfoView.text = formatRank(egfToRank(opponent.rating))
        Glide.with(viewHolder.iconView)
                .load(processGravatarURL(opponent.icon, 40.DP()))
                .listener(object: RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        return false

                    }

                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        viewHolder.iconView.setBackgroundDrawable(null)
                        return false
                    }
                })
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(viewHolder.iconView)
                .waitForLayout()
    }

    override fun unbind(viewHolder: GroupieViewHolder) {
        Glide.with(viewHolder.iconView).clear(viewHolder.iconView)
        super.unbind(viewHolder)
    }

    override fun getLayout() = R.layout.item_opponent

}