package io.zenandroid.onlinego.ui.items

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.viewbinding.BindableItem
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.databinding.ItemCarouselBinding

class CarouselItem(
        private val adapter: GroupAdapter<GroupieViewHolder>
) : BindableItem<ItemCarouselBinding>() {

    override fun bind(viewBinding: ItemCarouselBinding, position: Int) {
        viewBinding.recyclerView.layoutManager = LinearLayoutManager(viewBinding.recyclerView.context, LinearLayoutManager.HORIZONTAL, false)
        viewBinding.recyclerView.adapter = adapter
    }

    override fun getLayout() = R.layout.item_carousel
    override fun initializeViewBinding(view: View): ItemCarouselBinding = ItemCarouselBinding.bind(view)
}