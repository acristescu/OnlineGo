package io.zenandroid.onlinego.reusable

import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.model.local.Challenge

class ChallengeItem (val challenge: Challenge) : Item(challenge.id) {
    override fun bind(holder: ViewHolder, position: Int) {
    }

    override fun getLayout(): Int = R.layout.item_challenge
}