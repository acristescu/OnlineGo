package io.zenandroid.onlinego.reusable

import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.model.ogs.OGSAutomatch
import kotlinx.android.synthetic.main.item_automatch.*

class AutomatchItem(
        val automatch: OGSAutomatch,
        private val onAutomatchCancelled: ((OGSAutomatch) -> Unit)?
) : Item() {
    override fun bind(holder: GroupieViewHolder, position: Int) {
        val speed = automatch.size_speed_options?.get(0)?.speed?.capitalize()
        val sizes = automatch.size_speed_options?.joinToString(separator = "   ") { it.size }
        holder.details.text = "$speed   $sizes"
        holder.cancelButton.setOnClickListener { onAutomatchCancelled?.invoke(automatch) }
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        (other as? AutomatchItem)?.let {
            return it.automatch.uuid == automatch.uuid
        }

        return false
    }

    override fun getLayout(): Int = R.layout.item_automatch
}