package io.zenandroid.onlinego.ui.screens.chat

import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.utils.hide
import io.zenandroid.onlinego.utils.setMarginsDP
import io.zenandroid.onlinego.utils.show
import io.zenandroid.onlinego.data.model.local.Message
import io.zenandroid.onlinego.databinding.ItemMessageBinding
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class MessageItem(
        private val message : Message,
        private val myMessage : Boolean
        ) : BindableItem<ItemMessageBinding>() {
    private val sdf = SimpleDateFormat.getTimeInstance(DateFormat.SHORT)

    override fun bind(binding: ItemMessageBinding, position: Int) {
        binding.messageText.text = message.text
        binding.dateView.text = sdf.format(Date(message.date * 1000))

        val hiddenUntilSuffix = if(message.type == Message.Type.MALKOVITCH) " (hidden until game ends)" else ""
        binding.moveNo.text = "Move ${message.moveNumber.toString()}$hiddenUntilSuffix"
        binding.chatBubble.apply {
            when {
                myMessage -> {
                    binding.author.hide()
                    setMarginsDP(left = 50, right = 10)
                    ResourcesCompat.getColorStateList(resources, R.color.colorPrimaryLight, null)?.let {
                        setCardBackgroundColor(it)
                    }
                }
                message.type == Message.Type.SPECTATOR -> {
                    binding.author.show()
                    binding.author.text = "${message.username} (spectator)"
                    setMarginsDP(left = 10, right = 50)
                    ResourcesCompat.getColorStateList(resources, R.color.colorOffWhite, null)?.let {
                        setCardBackgroundColor(it)
                    }
                }
                else -> {
                    binding.author.hide()
                    setMarginsDP(left = 10, right = 50)
                    ResourcesCompat.getColorStateList(resources, R.color.colorTextBackground, null)?.let {
                        setCardBackgroundColor(it)
                    }
                }
            }
        }
    }

    override fun getLayout()
            = R.layout.item_message

    override fun isSameAs(other: Item<*>): Boolean {
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

    override fun initializeViewBinding(view: View): ItemMessageBinding = ItemMessageBinding.bind(view)
}