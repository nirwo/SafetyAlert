package nir.wolff.ui.groups

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import nir.wolff.R
import nir.wolff.databinding.FragmentGroupMembersBinding
import nir.wolff.model.Group
import nir.wolff.model.GroupMember

class GroupMembersFragment : Fragment() {
    private var _binding: FragmentGroupMembersBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var memberAdapter: GroupMemberAdapter? = null
    private lateinit var group: Group

    companion object {
        private const val TAG = "GroupMembersFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up RecyclerView
        binding.membersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        memberAdapter = GroupMemberAdapter(
            onDeleteClick = { member -> showDeleteMemberConfirmation(member) }
        )
        binding.membersRecyclerView.adapter = memberAdapter

        // Set up add member button
        binding.addMemberFab.setOnClickListener {
            showAddMemberDialog()
        }

        // Load group details
        val groupId = (activity as? GroupDetailsActivity)?.groupId
        if (groupId != null) {
            loadGroupDetails(groupId)
        } else {
            Log.e(TAG, "No group ID found")
            Toast.makeText(requireContext(), "Error loading group details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadGroupDetails(groupId: String) {
        firestore.collection("groups").document(groupId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error loading group details", e)
                    Toast.makeText(requireContext(), "Error loading group details", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    group = Group.fromDocument(snapshot)
                    updateUI()
                } else {
                    Toast.makeText(requireContext(), "Group not found", Toast.LENGTH_SHORT).show()
                    activity?.finish()
                }
            }
    }

    private fun updateUI() {
        memberAdapter?.submitList(group.members)
        memberAdapter?.isAdmin = auth.currentUser?.email == group.adminEmail
        binding.addMemberFab.visibility = if (auth.currentUser?.email == group.adminEmail) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun showAddMemberDialog() {
        val currentUser = auth.currentUser
        if (currentUser?.email != group.adminEmail) {
            Toast.makeText(requireContext(), "Only the group admin can add members", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_member, null)
        val emailInput = dialogView.findViewById<android.widget.EditText>(R.id.emailInput)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Member")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val email = emailInput.text.toString().trim()
                if (email.isNotEmpty()) {
                    addMember(email)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addMember(email: String) {
        val newMember = GroupMember(
            email = email,
            status = GroupMember.Status.UNKNOWN
        )

        val updatedMembers = group.members + newMember
        val updatedGroup = group.copy(members = updatedMembers)

        firestore.collection("groups").document(group.id)
            .set(updatedGroup.toMap())
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Member added successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding member", e)
                Toast.makeText(requireContext(), "Error adding member: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteMemberConfirmation(member: GroupMember) {
        val currentUser = auth.currentUser
        if (currentUser?.email != group.adminEmail) {
            Toast.makeText(requireContext(), "Only the group admin can remove members", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove Member")
            .setMessage("Are you sure you want to remove ${member.email} from the group?")
            .setPositiveButton("Remove") { _, _ ->
                removeMember(member)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeMember(member: GroupMember) {
        val updatedMembers = group.members.filter { it.email != member.email }
        val updatedGroup = group.copy(members = updatedMembers)

        firestore.collection("groups").document(group.id)
            .set(updatedGroup.toMap())
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Member removed successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error removing member", e)
                Toast.makeText(requireContext(), "Error removing member: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
