package io.zenandroid.onlinego.chat

import androidx.core.content.res.ResourcesCompat
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.hide
import io.zenandroid.onlinego.extensions.setMarginsDP
import io.zenandroid.onlinego.extensions.show
import io.zenandroid.onlinego.model.local.Message
import kotlinx.android.synthetic.main.item_message.*
import kotlinx.android.synthetic.main.item_message.view.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class MessageItem(
        private val message : Message,
        private val myMessage : Boolean
        ) : Item() {
    private val sdf = SimpleDateFormat.getTimeInstance(DateFormat.SHORT)

    override fun bind(holder: ViewHolder, position: Int) {
        holder.messageText.text = message.text
        holder.dateView.text = sdf.format(Date(message.date * 1000))

        val hiddenUntilSuffix = if(message.type == Message.Type.MALKOVITCH) " (hidden until game ends)" else ""
        holder.moveNo.text = "Move ${message.moveNumber.toString()}$hiddenUntilSuffix"
        holder.chatBubble.apply {
            when {
                myMessage -> {
                    author.hide()
                    setMarginsDP(left = 50, right = 10)
                    ResourcesCompat.getColorStateList(resources, R.color.colorPrimaryLight, null)?.let {
                        setCardBackgroundColor(it)
                    }
                }
                message.type == Message.Type.SPECTATOR -> {
                    author.show()
                    author.text = "${message.username} (spectator)"
                    setMarginsDP(left = 10, right = 50)
                    ResourcesCompat.getColorStateList(resources, R.color.colorOffWhite, null)?.let {
                        setCardBackgroundColor(it)
                    }
                }
                else -> {
                    author.hide()
                    setMarginsDP(left = 10, right = 50)
                    ResourcesCompat.getColorStateList(resources, R.color.white, null)?.let {
                        setCardBackgroundColor(it)
                    }
                }
            }
        }
    }

    override fun getLayout()
            = R.layout.item_message

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        if (other !is MessageItem) {
            return false
        }
        return other.message.chatId == message.chatId
    }

    override fun equals(other: Any?): Boolean {
        if (other !is MessageItem) {
            return false
        }
        return other.message == message
    }

    override fun hashCode(): Int {
        return message.hashCode()
    }
}