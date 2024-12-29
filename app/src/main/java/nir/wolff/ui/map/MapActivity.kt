package nir.wolff.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nir.wolff.R
import nir.wolff.databinding.ActivityMapBinding
import nir.wolff.model.AlertEvent
import nir.wolff.model.GeoLocation
import nir.wolff.model.Group
import nir.wolff.model.GroupMember
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMapBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val firestore = FirebaseFirestore.getInstance()
    private val client = OkHttpClient()
    private var groupMembersListener: ListenerRegistration? = null
    private var alertsListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "MapActivity"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val PIKUD_HAOREF_API_URL = "https://www.oref.org.il/WarningMessages/alert/alerts.json"
        private const val DEFAULT_ZOOM = 12f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Safety Map"

        // Set up toolbar navigation
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set up the map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Set up refresh button
        binding.refreshButton.setOnClickListener {
            refreshAlerts()
        }

        checkLocationPermission()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isMyLocationButtonEnabled = true
            isMapToolbarEnabled = false
            isCompassEnabled = true
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            
            // Get current location and move camera
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
        
        setupGroupMembersListener()
        setupAlertsListener()
        refreshAlerts()
    }

    private fun setupAlertsListener() {
        alertsListener = firestore.collection("alerts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                map.clear()
                snapshot?.documents?.forEach { doc ->
                    val alert = AlertEvent.fromMap(doc.data ?: return@forEach, doc.id)
                    // Add circle for the alert area
                    map.addCircle(
                        CircleOptions()
                            .center(LatLng(alert.location.latitude, alert.location.longitude))
                            .radius(2000.0) // 2km radius
                            .strokeColor(Color.RED)
                            .fillColor(Color.argb(70, 255, 0, 0))
                    )

                    // Add marker for the alert
                    map.addMarker(
                        MarkerOptions()
                            .position(LatLng(alert.location.latitude, alert.location.longitude))
                            .title(alert.userEmail)
                            .snippet("Status: ${alert.status}\nTime: ${Date(alert.timestamp)}")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                }
            }
    }

    private fun setupGroupMembersListener() {
        val groupId = intent.getStringExtra("group_id") ?: return
        
        groupMembersListener = firestore.collection("groups")
            .document(groupId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                val group = snapshot?.let { Group.fromDocument(it) }
                group?.members?.forEach { member ->
                    member.lastLocation?.let { location ->
                        val markerColor = when (member.status) {
                            GroupMember.Status.SAFE -> BitmapDescriptorFactory.HUE_GREEN
                            GroupMember.Status.UNSAFE -> BitmapDescriptorFactory.HUE_RED
                            else -> BitmapDescriptorFactory.HUE_YELLOW
                        }

                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(location.latitude, location.longitude))
                                .title(member.email)
                                .snippet("Status: ${member.status}")
                                .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                        )
                    }
                }
            }
    }

    private fun refreshAlerts() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(PIKUD_HAOREF_API_URL)
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .addHeader("Referer", "https://www.oref.org.il/")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val responseBody = response.body?.string()
                    val json = JSONObject(responseBody ?: "{}")
                    val alerts = json.optJSONArray("data") ?: return@launch

                    withContext(Dispatchers.Main) {
                        for (i in 0 until alerts.length()) {
                            val alert = alerts.getJSONObject(i)
                            val location = alert.optJSONObject("location")
                            if (location != null) {
                                val latLng = LatLng(
                                    location.optDouble("lat", 0.0),
                                    location.optDouble("lng", 0.0)
                                )

                                // Add circle for the alert area
                                map.addCircle(
                                    CircleOptions()
                                        .center(latLng)
                                        .radius(2000.0) // 2km radius
                                        .strokeColor(Color.RED)
                                        .fillColor(Color.argb(70, 255, 0, 0))
                                )

                                // Add marker for the alert
                                map.addMarker(
                                    MarkerOptions()
                                        .position(latLng)
                                        .title(alert.optString("title", "Alert"))
                                        .snippet(alert.optString("description", ""))
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing alerts", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MapActivity,
                        "Failed to refresh alerts: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    if (::map.isInitialized) {
                        if (ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            map.isMyLocationEnabled = true
                        }
                    }
                }
                return
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        groupMembersListener?.remove()
        alertsListener?.remove()
    }
}
