package io.zenandroid.onlinego.ui.screens.chat

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.Message
import io.zenandroid.onlinego.data.repositories.ChatRepository
import kotlinx.android.synthetic.main.dialog_messages.*
import org.koin.android.ext.android.inject

class ChatDialog : DialogFragment() {

    private val chatRepository: ChatRepository by inject()

    private val messagesAdapter = MessagesAdapter()
    private val sendMessageSubject = PublishSubject.create<String>()
    private var messages: List<Message> = listOf()

    val sendMessage: Observable<String> = sendMessageSubject.hide()

    fun setMessages(messages: List<Message>) {
        if(dialog?.isShowing == true) {
            markMessagesAsRead()
        }
        this.messages = messages
        messagesAdapter.setMessageList(messages)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        isCancelable = true
        setStyle(STYLE_NORMAL, 0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return inflater.inflate(R.layout.dialog_messages, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        messagesRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = messagesAdapter
            smoothScrollToPosition(messagesAdapter.itemCount)
        }

        sendButton.setOnClickListener {
            if(!newMessage.text.isNullOrEmpty()) {
                sendMessageSubject.onNext(newMessage.text.toString())
            }
            newMessage.setText("")
        }

        closeButton.setOnClickListener {
            dismiss()
        }
    }

    fun setChatMyId(myId: Long?) {
        messagesAdapter.setMyId(myId)
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.attributes?.let {
            it.width = ViewGroup.LayoutParams.MATCH_PARENT
            it.height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog?.window?.attributes = it
        }

        markMessagesAsRead()
    }

    private fun markMessagesAsRead() {
        chatRepository.markMessagesAsRead(messages.filter { !it.seen })
    }
}