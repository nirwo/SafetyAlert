package nir.wolff.ui.groups

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import nir.wolff.databinding.FragmentGroupChatBinding
import nir.wolff.model.ChatMessage

class GroupChatFragment : Fragment() {
    private var _binding: FragmentGroupChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatAdapter: GroupChatAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var groupId: String = ""

    companion object {
        private const val TAG = "GroupChatFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupId = (activity as? GroupDetailsActivity)?.groupId ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSendButton()
        listenForMessages()
    }

    private fun setupRecyclerView() {
        chatAdapter = GroupChatAdapter(auth.currentUser?.email ?: "")
        binding.messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupSendButton() {
        binding.sendButton.setOnClickListener {
            val message = binding.messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                binding.messageInput.text?.clear()
            }
        }
    }

    private fun sendMessage(message: String) {
        val currentUser = auth.currentUser ?: return
        val chatMessage = ChatMessage(
            groupId = groupId,
            senderEmail = currentUser.email ?: return,
            message = message
        )

        firestore.collection("chats")
            .add(chatMessage)
            .addOnSuccessListener {
                Log.d(TAG, "Message sent successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error sending message", e)
                Toast.makeText(context, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenForMessages() {
        firestore.collection("chats")
            .whereEqualTo("groupId", groupId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error listening for messages", e)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        ChatMessage(
                            id = doc.id,
                            groupId = doc.getString("groupId") ?: "",
                            senderEmail = doc.getString("senderEmail") ?: "",
                            message = doc.getString("message") ?: "",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting message doc", e)
                        null
                    }
                } ?: emptyList()

                chatAdapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    binding.messagesRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
