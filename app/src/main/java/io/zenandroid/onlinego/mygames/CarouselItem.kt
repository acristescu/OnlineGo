package io.zenandroid.onlinego.mygames

import android.support.v7.widget.LinearLayoutManager
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.zenandroid.onlinego.R
import kotlinx.android.synthetic.main.item_carousel.*

class CarouselItem(
        private val adapter: GroupAdapter<ViewHolder>
) : Item() {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.recycler_view.layoutManager = LinearLayoutManager(viewHolder.recycler_view.context, LinearLayoutManager.HORIZONTAL, false)
        viewHolder.recycler_view.adapter = adapter
    }

    override fun getLayout() = R.layout.item_carousel
}