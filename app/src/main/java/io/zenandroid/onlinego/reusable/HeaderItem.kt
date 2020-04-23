package io.zenandroid.onlinego.reusable

import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import io.zenandroid.onlinego.R
import kotlinx.android.synthetic.main.section_header.*

/**
 * Created by alex on 31/05/2018.
 */
class HeaderItem(val title: String) : Item() {
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.sectionTitle.text = title
    }

    override fun getLayout(): Int =
            R.layout.section_header

}
