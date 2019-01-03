package io.zenandroid.onlinego.reusable

import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.hide
import io.zenandroid.onlinego.extensions.show
import io.zenandroid.onlinego.model.local.Challenge
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import kotlinx.android.synthetic.main.item_challenge.*

class ChallengeItem(
        val challenge: Challenge,
        private val onChallengeCancelled: ((Challenge) -> Unit)?,
        private val onChallengeAccepted: ((Challenge) -> Unit)?,
        private val onChallengeDeclined: ((Challenge) -> Unit)?
) : Item(challenge.id) {
    override fun bind(holder: ViewHolder, position: Int) {
        if (challenge.challenger?.id == OGSServiceImpl.uiConfig?.user?.id) {
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