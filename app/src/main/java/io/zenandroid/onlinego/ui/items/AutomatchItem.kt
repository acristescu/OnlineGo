package io.zenandroid.onlinego.ui.items

import android.view.View
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.databinding.ItemAutomatchBinding

class AutomatchItem(
        val automatch: OGSAutomatch,
        private val onAutomatchCancelled: ((OGSAutomatch) -> Unit)?
) : BindableItem<ItemAutomatchBinding>() {
    override fun bind(viewBinding: ItemAutomatchBinding, position: Int) {
        val speed = automatch.size_speed_options?.get(0)?.speed?.capitalize()
        val sizes = automatch.size_speed_options?.joinToString(separator = "   ") { it.size }
        viewBinding.details.text = "$speed   $sizes"
        viewBinding.cancelButton.setOnClickListener { onAutomatchCancelled?.invoke(automatch) }
    }

    override fun isSameAs(other: Item<*>): Boolean {
        (other as? AutomatchItem)?.let {
            return it.automatch.uuid == automatch.uuid
        }

        return false
    }

    override fun getLayout(): Int = R.layout.item_automatch
    override fun initializeViewBinding(view: View): ItemAutomatchBinding = ItemAutomatchBinding.bind(view)
}