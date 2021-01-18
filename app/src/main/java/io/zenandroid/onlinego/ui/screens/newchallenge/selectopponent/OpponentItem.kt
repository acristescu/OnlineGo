package io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent

import android.graphics.drawable.Drawable
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.xwray.groupie.viewbinding.BindableItem
import com.xwray.groupie.viewbinding.GroupieViewHolder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.utils.DP
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.databinding.ItemOpponentBinding
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import io.zenandroid.onlinego.utils.processGravatarURL


class OpponentItem(
        val opponent: Player
) : BindableItem<ItemOpponentBinding>(opponent.id) {
    override fun bind(binding: ItemOpponentBinding, position: Int) {
        binding.nameView.text = opponent.username
        binding.extraInfoView.text = formatRank(egfToRank(opponent.rating))
        Glide.with(binding.iconView)
                .load(processGravatarURL(opponent.icon, 40.DP()))
                .listener(object: RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        return false

                    }

                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        binding.iconView.setBackgroundDrawable(null)
                        return false
                    }
                })
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(binding.iconView)
                .waitForLayout()
    }

    override fun unbind(viewHolder: GroupieViewHolder<ItemOpponentBinding>) {
        Glide.with(viewHolder.binding.iconView).clear(viewHolder.binding.iconView)
        super.unbind(viewHolder)
    }

    override fun getLayout() = R.layout.item_opponent
    override fun initializeViewBinding(view: View): ItemOpponentBinding = ItemOpponentBinding.bind(view)

}