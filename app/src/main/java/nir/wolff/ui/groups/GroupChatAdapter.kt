package nir.wolff.ui.groups

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import nir.wolff.databinding.ItemChatMessageBinding
import nir.wolff.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

class GroupChatAdapter(private val currentUserEmail: String) :
    ListAdapter<ChatMessage, GroupChatAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = getItem(position)
        holder.bind(message)
    }

    inner class MessageViewHolder(private val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            val isSentByMe = message.senderEmail == currentUserEmail

            // Set message text and sender
            binding.messageText.text = message.message
            binding.senderText.text = if (isSentByMe) "You" else message.senderEmail

            // Set timestamp
            binding.timestampText.text = formatTimestamp(message.timestamp)

            // Align message based on sender
            if (isSentByMe) {
                binding.messageContainer.layoutDirection = View.LAYOUT_DIRECTION_RTL
                binding.messageCard.setCardBackgroundColor(0xFF2196F3.toInt()) // Blue for sent messages
                binding.messageText.setTextColor(0xFFFFFFFF.toInt()) // White text
                binding.senderText.setTextColor(0xFFFFFFFF.toInt()) // White text
                binding.timestampText.setTextColor(0xFFFFFFFF.toInt()) // White text
            } else {
                binding.messageContainer.layoutDirection = View.LAYOUT_DIRECTION_LTR
                binding.messageCard.setCardBackgroundColor(0xFFE0E0E0.toInt()) // Grey for received messages
                binding.messageText.setTextColor(0xFF000000.toInt()) // Black text
                binding.senderText.setTextColor(0xFF000000.toInt()) // Black text
                binding.timestampText.setTextColor(0xFF000000.toInt()) // Black text
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}
