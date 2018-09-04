package io.zenandroid.onlinego.chat

import android.support.v7.widget.RecyclerView
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import io.zenandroid.onlinego.model.local.Message

class MessagesAdapter : GroupAdapter<ViewHolder>() {
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