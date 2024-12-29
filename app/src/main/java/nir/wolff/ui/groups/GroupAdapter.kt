package nir.wolff.ui.groups

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import nir.wolff.databinding.ItemGroupBinding
import nir.wolff.model.Group
import nir.wolff.model.GroupMember

class GroupAdapter(
    private val onGroupClick: (Group) -> Unit,
    private val onMemberClick: (Group, GroupMember) -> Unit,
    private val onSafetyStatusChange: (Group, Boolean) -> Unit,
    private val onMapClick: (Group) -> Unit,
    private val onDeleteGroup: ((Group) -> Unit)? = null
) : ListAdapter<Group, GroupAdapter.GroupViewHolder>(GroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GroupViewHolder(
        private val binding: ItemGroupBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onGroupClick(getItem(position))
                }
            }

            binding.mapButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onMapClick(getItem(position))
                }
            }

            binding.safetySwitch.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSafetyStatusChange(getItem(position), isChecked)
                }
            }

            binding.deleteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteGroup?.invoke(getItem(position))
                }
            }
        }

        fun bind(group: Group) {
            binding.groupNameText.text = group.name
            
            // Setup members recycler view
            val membersAdapter = GroupMembersAdapter { member ->
                onMemberClick(group, member)
            }
            binding.membersRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = membersAdapter
            }
            membersAdapter.submitList(group.members)

            // Set safety switch state
            val currentUserEmail = getCurrentUserEmail()
            val currentMember = group.members.firstOrNull { it.email == currentUserEmail }
            binding.safetySwitch.isChecked = currentMember?.status == GroupMember.Status.SAFE

            // Show delete button only for admin
            binding.deleteButton.visibility = if (currentUserEmail == group.adminEmail) View.VISIBLE else View.GONE
        }

        private fun getCurrentUserEmail(): String? {
            return FirebaseAuth.getInstance().currentUser?.email
        }
    }

    class GroupDiffCallback : DiffUtil.ItemCallback<Group>() {
        override fun areItemsTheSame(oldItem: Group, newItem: Group): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Group, newItem: Group): Boolean {
            return oldItem == newItem
        }
    }
}
