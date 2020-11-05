package io.zenandroid.onlinego.ui.items

import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.utils.hide
import io.zenandroid.onlinego.utils.show
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.gamelogic.Util.getCurrentUserId
import kotlinx.android.synthetic.main.item_challenge.*

class ChallengeItem(
        val challenge: Challenge,
        private val onChallengeCancelled: ((Challenge) -> Unit)?,
        private val onChallengeAccepted: ((Challenge) -> Unit)?,
        private val onChallengeDeclined: ((Challenge) -> Unit)?
) : Item(challenge.id) {
    override fun bind(holder: GroupieViewHolder, position: Int) {
        if (challenge.challenger?.id == getCurrentUserId()) {
            holder.title.text = "You are challenging ${challenge.challenged?.username}"
            holder.acceptButton.hide()
            holder.declineButton.hide()
            holder.cancelButton.show()
        } else {
            holder.title.text = "${challenge.challenger?.username} is challenging you"
            holder.acceptButton.show()
            holder.declineButton.show()
            holder.cancelButton.hide()
        }

        holder.acceptButton.setOnClickListener { onChallengeAccepted?.invoke(challenge) }
        holder.cancelButton.setOnClickListener { onChallengeCancelled?.invoke(challenge) }
        holder.declineButton.setOnClickListener { onChallengeDeclined?.invoke(challenge) }
    }

    override fun getLayout(): Int = R.layout.item_challenge
}