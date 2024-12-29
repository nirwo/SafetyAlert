package nir.wolff.ui.main

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import nir.wolff.R
import nir.wolff.databinding.ActivityMainBinding
import nir.wolff.model.AlertEvent
import nir.wolff.model.Group
import nir.wolff.model.GroupMember
import nir.wolff.model.GeoLocation
import nir.wolff.ui.auth.SignInActivity
import nir.wolff.ui.groups.CreateGroupActivity
import nir.wolff.ui.groups.GroupAdapter
import nir.wolff.ui.groups.GroupDetailsActivity
import nir.wolff.ui.map.MapActivity
import nir.wolff.ui.profile.ProfileActivity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var groupAdapter: GroupAdapter
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var groupsListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "MainActivity"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupFab()
        setupSignInButton()
        setupSignOutButton()
        checkAuthState()
    }

    private fun setupSignInButton() {
        binding.signInButton.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }

    private fun setupSignOutButton() {
        binding.signOutButton.setOnClickListener {
            auth.signOut()
            checkAuthState()
        }

        binding.profileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun updateUIForAuthState(isSignedIn: Boolean) {
        if (isSignedIn) {
            val user = auth.currentUser
            binding.welcomeText.text = "Welcome to Safety Alert"
            binding.userEmailText.text = user?.email ?: ""
            binding.welcomeCard.visibility = View.VISIBLE
            binding.signInButton.visibility = View.GONE
            binding.signOutButton.visibility = View.VISIBLE
            binding.createGroupFab.visibility = View.VISIBLE
            binding.groupsRecyclerView.visibility = View.VISIBLE
            binding.bottomActions.visibility = View.VISIBLE
        } else {
            binding.welcomeCard.visibility = View.GONE
            binding.signInButton.visibility = View.VISIBLE
            binding.signOutButton.visibility = View.GONE
            binding.createGroupFab.visibility = View.GONE
            binding.groupsRecyclerView.visibility = View.GONE
            binding.bottomActions.visibility = View.GONE
        }
        binding.noGroupsText.visibility = View.GONE
    }

    private fun checkAuthState() {
        val currentUser = auth.currentUser
        Log.d(TAG, "Checking auth state. Current user: ${currentUser?.email}")
        
        if (currentUser == null) {
            updateUIForAuthState(false)
            return
        }

        updateUIForAuthState(true)
        loadGroups()
    }

    override fun onResume() {
        super.onResume()
        checkAuthState()
    }

    private fun setupRecyclerView() {
        groupAdapter = GroupAdapter(
            onGroupClick = { group ->
                val intent = Intent(this, GroupDetailsActivity::class.java).apply {
                    putExtra("group_id", group.id)
                }
                startActivity(intent)
            },
            onMemberClick = { group, member ->
                // Handle member click if needed
            },
            onSafetyStatusChange = { group, isSafe ->
                updateSafetyStatus(group, isSafe)
            },
            onMapClick = { group ->
                val intent = Intent(this, MapActivity::class.java).apply {
                    putExtra("group_id", group.id)
                }
                startActivity(intent)
            },
            onDeleteGroup = { group ->
                showDeleteGroupConfirmation(group)
            }
        )

        binding.groupsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = groupAdapter
            addItemDecoration(DividerItemDecoration(this@MainActivity, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupFab() {
        binding.createGroupFab.setOnClickListener {
            startActivity(Intent(this, CreateGroupActivity::class.java))
        }
    }

    private fun loadGroups() {
        val currentUser = auth.currentUser?.email ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.groupsRecyclerView.visibility = View.GONE
        binding.noGroupsText.visibility = View.GONE

        groupsListener = firestore.collection("groups")
            .whereArrayContains("members", mapOf("email" to currentUser))
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error loading groups", e)
                    binding.progressBar.visibility = View.GONE
                    binding.noGroupsText.visibility = View.VISIBLE
                    binding.groupsRecyclerView.visibility = View.GONE
                    Toast.makeText(this, "Failed to load groups: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                binding.progressBar.visibility = View.GONE
                if (snapshot == null || snapshot.isEmpty) {
                    binding.noGroupsText.visibility = View.VISIBLE
                    binding.groupsRecyclerView.visibility = View.GONE
                    return@addSnapshotListener
                }

                val groups = snapshot.documents.mapNotNull { doc ->
                    try {
                        Group.fromDocument(doc)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing group", e)
                        null
                    }
                }

                if (groups.isEmpty()) {
                    binding.noGroupsText.visibility = View.VISIBLE
                    binding.groupsRecyclerView.visibility = View.GONE
                } else {
                    binding.noGroupsText.visibility = View.GONE
                    binding.groupsRecyclerView.visibility = View.VISIBLE
                    groupAdapter.submitList(groups)
                }
            }
    }

    private fun updateSafetyStatus(group: Group, isSafe: Boolean) {
        val currentUser = auth.currentUser ?: return
        val userEmail = currentUser.email ?: return

        lifecycleScope.launch {
            try {
                // Get current location
                val location = getCurrentLocation()

                // Create alert event with location
                val alertEvent = AlertEvent(
                    groupId = group.id,
                    userEmail = userEmail,
                    eventType = if (isSafe) AlertEvent.EventType.ALL_CLEAR else AlertEvent.EventType.NEED_HELP,
                    message = if (isSafe) "I'm safe" else "I need help",
                    location = GeoLocation(
                        latitude = location?.latitude ?: 0.0,
                        longitude = location?.longitude ?: 0.0
                    )
                )

                // Add alert to Firestore
                firestore.collection("alerts")
                    .add(alertEvent.toMap())
                    .await()

                // Update member status in group
                val updatedMembers = group.members.map { member ->
                    if (member.email == userEmail) {
                        member.copy(
                            status = if (isSafe) GroupMember.Status.SAFE else GroupMember.Status.UNSAFE,
                            lastLocation = location?.let { loc ->
                                GeoLocation(
                                    latitude = loc.latitude,
                                    longitude = loc.longitude
                                ).toMap()
                            }
                        )
                    } else {
                        member
                    }
                }

                // Update group in Firestore
                val updatedGroup = group.copy(members = updatedMembers)
                firestore.collection("groups")
                    .document(group.id)
                    .set(updatedGroup.toMap())
                    .await()

                Toast.makeText(
                    this@MainActivity,
                    if (isSafe) "Marked as safe" else "Alert sent",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating safety status", e)
                Toast.makeText(
                    this@MainActivity,
                    "Failed to update status: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun getCurrentLocation(): Location? {
        return try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
                return null
            }

            val fusedLocationClient: FusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
            null
        }
    }

    private fun showDeleteGroupConfirmation(group: Group) {
        val currentUser = auth.currentUser
        if (currentUser?.email != group.adminEmail) {
            Toast.makeText(this, "Only the group admin can delete the group", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Group")
            .setMessage("Are you sure you want to delete ${group.name}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteGroup(group)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteGroup(group: Group) {
        firestore.collection("groups")
            .document(group.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Group deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting group", e)
                Toast.makeText(this, "Failed to delete group: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        groupsListener?.remove()
    }
}
