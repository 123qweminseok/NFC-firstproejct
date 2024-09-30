package com.minseok.tel
// Message.kt

// MessageAdapter.kt
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.minseok.tel.R

class MessageAdapter(private val onDeleteClick: (Message) -> Unit) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    private val messages = mutableListOf<Message>()

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun removeMessage(messageId: String) {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            messages.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount() = messages.size

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val senderTextView: TextView = itemView.findViewById(R.id.senderTextView)
        private val phoneTextView: TextView = itemView.findViewById(R.id.phoneTextView)
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        private val deleteButton: View = itemView.findViewById(R.id.deleteButton)

        fun bind(message: Message) {
            senderTextView.text = "보낸사람: ${message.sender}"
            phoneTextView.text = "번호: ${message.phone}"  // 전화번호만 표시
            messageTextView.text ="내용 ${message.message}"  // "메시지: " 접두어 제거
            deleteButton.setOnClickListener { onDeleteClick(message)}
        }
    }
}
