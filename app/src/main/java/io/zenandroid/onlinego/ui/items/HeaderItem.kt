package io.zenandroid.onlinego.ui.items

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.databinding.SectionHeaderBinding

/**
 * Created by alex on 31/05/2018.
 */
class HeaderItem(val title: String) : BindableItem<SectionHeaderBinding>() {
    override fun bind(binding: SectionHeaderBinding, position: Int) {
        binding.sectionTitle.text = title
    }

    override fun getLayout(): Int =
            R.layout.section_header

    override fun initializeViewBinding(view: View): SectionHeaderBinding = SectionHeaderBinding.bind(view)

}
