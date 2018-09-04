package io.zenandroid.onlinego.chat

import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import io.zenandroid.onlinego.model.local.Message

class MessagesAdapter : GroupAdapter<ViewHolder>() {
    private val messagesSection = Section()
    private var myId : Long? = null

    fun setMessageList(messages : List<Message>) {
        messagesSection.update(messages.map { MessageItem(it, myId == it.playerId) })
    }

    fun setMyId(myId: Long?) {
        this.myId = myId
    }

    init {
        add(messagesSection)
    }
}