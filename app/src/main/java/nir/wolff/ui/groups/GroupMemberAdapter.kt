package nir.wolff.ui.groups

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import nir.wolff.databinding.ItemGroupMemberBinding
import nir.wolff.model.GroupMember

class GroupMemberAdapter(
    private val onDeleteClick: (GroupMember) -> Unit,
    var isAdmin: Boolean = false
) : ListAdapter<GroupMember, GroupMemberAdapter.ViewHolder>(GroupMemberDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGroupMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemGroupMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: GroupMember) {
            binding.apply {
                memberEmail.text = member.email
                memberStatus.text = when (member.status) {
                    GroupMember.Status.SAFE -> "Safe"
                    GroupMember.Status.UNSAFE -> "Needs Help"
                    GroupMember.Status.UNKNOWN -> "Unknown"
                }

                removeMemberButton.visibility = if (isAdmin) View.VISIBLE else View.GONE
                removeMemberButton.setOnClickListener {
                    onDeleteClick(member)
                }
            }
        }
    }

    private class GroupMemberDiffCallback : DiffUtil.ItemCallback<GroupMember>() {
        override fun areItemsTheSame(oldItem: GroupMember, newItem: GroupMember): Boolean {
            return oldItem.email == newItem.email
        }

        override fun areContentsTheSame(oldItem: GroupMember, newItem: GroupMember): Boolean {
            return oldItem == newItem
        }
    }
}
