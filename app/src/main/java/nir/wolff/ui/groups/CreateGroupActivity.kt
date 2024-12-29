package nir.wolff.ui.groups

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import nir.wolff.databinding.ActivityCreateGroupBinding
import nir.wolff.model.Group
import nir.wolff.model.GroupMember

class CreateGroupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateGroupBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "CreateGroupActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Create Group"

        // Set up toolbar navigation
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Set up create button
        binding.createButton.setOnClickListener {
            createGroup()
        }
    }

    private fun createGroup() {
        val groupName = binding.groupNameInput.text.toString().trim()
        if (groupName.isEmpty()) {
            binding.groupNameInput.error = "Please enter a group name"
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Create admin member
        val adminMember = GroupMember(
            email = currentUser.email ?: "",
            name = currentUser.displayName ?: currentUser.email ?: "",
            status = GroupMember.Status.UNKNOWN
        )

        // Create group
        val group = Group(
            name = groupName,
            adminEmail = currentUser.email ?: "",
            members = listOf(adminMember)
        )

        // Add to Firestore
        firestore.collection("groups")
            .add(group.toMap())
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Group created with ID: ${documentReference.id}")
                Toast.makeText(this, "Group created successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating group", e)
                Toast.makeText(this, "Failed to create group: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
