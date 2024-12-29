package nir.wolff.ui.groups

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import nir.wolff.R
import nir.wolff.databinding.ActivityGroupDetailsBinding
import nir.wolff.model.Group
import nir.wolff.model.GroupMember
import nir.wolff.model.GeoLocation

class GroupDetailsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityGroupDetailsBinding
    public lateinit var groupId: String
    private lateinit var group: Group
    private lateinit var map: GoogleMap
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var memberAdapter: GroupMemberAdapter? = null

    companion object {
        private const val TAG = "GroupDetailsActivity"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val DEFAULT_ZOOM = 12f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupId = intent.getStringExtra("groupId") ?: run {
            Toast.makeText(this, "Invalid group ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set up toolbar navigation
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Set up RecyclerView
        binding.membersRecyclerView.layoutManager = LinearLayoutManager(this)
        memberAdapter = GroupMemberAdapter(
            onDeleteClick = { member -> showDeleteMemberConfirmation(member) },
            isAdmin = auth.currentUser?.email == group.adminEmail
        )
        binding.membersRecyclerView.adapter = memberAdapter

        // Set up map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.miniMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        loadGroupDetails()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isMapToolbarEnabled = false
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            
            // Get current location and move camera
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM))
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error getting location", e)
                // Default to Israel location if can't get current location
                val defaultLocation = LatLng(31.7683, 35.2137)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 7f))
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            // Default to Israel location if no permission
            val defaultLocation = LatLng(31.7683, 35.2137)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 7f))
        }

        map.setOnMapLoadedCallback {
            // Map is loaded and ready
            updateMembersOnMap()
        }
    }

    private fun updateMembersOnMap() {
        if (!::map.isInitialized || !::group.isInitialized) return
        
        map.clear()
        group.members.forEach { member ->
            member.lastLocation?.let { location ->
                val position = LatLng(location.latitude, location.longitude)
                map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(member.email)
                        .snippet("Status: ${member.status}")
                )
            }
        }
    }

    private fun loadGroupDetails() {
        firestore.collection("groups").document(groupId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error loading group details", e)
                    Toast.makeText(this, "Error loading group details", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    group = Group.fromDocument(snapshot)
                    updateUI()
                } else {
                    Toast.makeText(this, "Group not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
    }

    private fun updateUI() {
        supportActionBar?.title = group.name
        memberAdapter?.submitList(group.members)
        memberAdapter?.isAdmin = auth.currentUser?.email == group.adminEmail
        updateMembersOnMap()
    }

    private fun showDeleteMemberConfirmation(member: GroupMember) {
        val currentUser = auth.currentUser
        if (currentUser?.email != group.adminEmail) {
            Toast.makeText(this, "Only the group admin can remove members", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(this)
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

        firestore.collection("groups").document(groupId)
            .set(updatedGroup.toMap())
            .addOnSuccessListener {
                Toast.makeText(this, "Member removed successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error removing member", e)
                Toast.makeText(this, "Error removing member: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        map.isMyLocationEnabled = true
                    }
                }
            }
        }
    }
}
