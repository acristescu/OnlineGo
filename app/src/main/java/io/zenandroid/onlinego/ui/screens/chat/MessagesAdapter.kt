package io.zenandroid.onlinego.ui.screens.chat

import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import io.zenandroid.onlinego.data.model.local.Message

class MessagesAdapter : GroupAdapter<GroupieViewHolder>() {
    private val messagesSection = object : Section() {
        override fun notifyItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.notifyItemRangeInserted(positionStart, itemCount)
            recyclerView?.smoothScrollToPosition(positionStart)
        }
    }
    private var myId : Long? = null
    private var recyclerView: RecyclerView? = null

    fun setMessageList(messages : List<Message>) {
        messagesSection.update(messages.map { MessageItem(it, myId == it.playerId) })
    }

    fun setMyId(myId: Long?) {
        this.myId = myId
    }

    init {
        add(messagesSection)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
        super.onDetachedFromRecyclerView(recyclerView)
    }
}