package nir.wolff.ui.groups

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import nir.wolff.databinding.ItemGroupMemberBinding
import nir.wolff.model.GroupMember

class GroupMembersAdapter(
    private val onRemoveMember: (GroupMember) -> Unit
) : ListAdapter<GroupMember, GroupMembersAdapter.ViewHolder>(DiffCallback()) {

    private var isAdmin = false

    fun setAdminStatus(isAdmin: Boolean) {
        this.isAdmin = isAdmin
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGroupMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val member = getItem(position)
        holder.bind(member, isAdmin, onRemoveMember)
    }

    inner class ViewHolder(
        private val binding: ItemGroupMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // onMemberClick(getItem(position)) // This line was removed in the provided code edit
                }
            }
        }

        fun bind(
            member: GroupMember,
            isAdmin: Boolean,
            onRemoveMember: (GroupMember) -> Unit
        ) {
            binding.apply {
                memberEmail.text = member.email
                memberStatus.text = when (member.status) {
                    GroupMember.Status.SAFE -> "Safe"
                    GroupMember.Status.UNSAFE -> "Needs Help"
                    GroupMember.Status.UNKNOWN -> "Unknown"
                }
                
                memberStatus.setTextColor(
                    when (member.status) {
                        GroupMember.Status.SAFE -> 
                            binding.root.context.getColor(android.R.color.holo_green_dark)
                        GroupMember.Status.UNSAFE -> 
                            binding.root.context.getColor(android.R.color.holo_red_dark)
                        GroupMember.Status.UNKNOWN -> 
                            binding.root.context.getColor(android.R.color.darker_gray)
                    }
                )
                
                // Show remove button only if current user is admin
                removeMemberButton.visibility = if (isAdmin) View.VISIBLE else View.GONE
                removeMemberButton.setOnClickListener {
                    onRemoveMember(member)
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<GroupMember>() {
        override fun areItemsTheSame(oldItem: GroupMember, newItem: GroupMember): Boolean {
            return oldItem.email == newItem.email
        }

        override fun areContentsTheSame(oldItem: GroupMember, newItem: GroupMember): Boolean {
            return oldItem == newItem
        }
    }
}
