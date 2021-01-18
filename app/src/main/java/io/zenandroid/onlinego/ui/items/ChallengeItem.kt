package io.zenandroid.onlinego.ui.items

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.utils.hide
import io.zenandroid.onlinego.utils.show
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.databinding.ItemChallengeBinding
import io.zenandroid.onlinego.gamelogic.Util.getCurrentUserId

class ChallengeItem(
        val challenge: Challenge,
        private val onChallengeCancelled: ((Challenge) -> Unit)?,
        private val onChallengeAccepted: ((Challenge) -> Unit)?,
        private val onChallengeDeclined: ((Challenge) -> Unit)?
) : BindableItem<ItemChallengeBinding>(challenge.id) {
    override fun bind(binding: ItemChallengeBinding, position: Int) {
        if (challenge.challenger?.id == getCurrentUserId()) {
            binding.title.text = "You are challenging ${challenge.challenged?.username}"
            binding.acceptButton.hide()
            binding.declineButton.hide()
            binding.cancelButton.show()
        } else {
            binding.title.text = "${challenge.challenger?.username} is challenging you"
            binding.acceptButton.show()
            binding.declineButton.show()
            binding.cancelButton.hide()
        }

        binding.acceptButton.setOnClickListener { onChallengeAccepted?.invoke(challenge) }
        binding.cancelButton.setOnClickListener { onChallengeCancelled?.invoke(challenge) }
        binding.declineButton.setOnClickListener { onChallengeDeclined?.invoke(challenge) }
    }

    override fun getLayout(): Int = R.layout.item_challenge
    override fun initializeViewBinding(view: View): ItemChallengeBinding = ItemChallengeBinding.bind(view)
}