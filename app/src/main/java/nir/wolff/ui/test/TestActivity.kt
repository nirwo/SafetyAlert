package nir.wolff.ui.test

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import nir.wolff.R
import nir.wolff.api.PikudHaorefApi
import nir.wolff.service.AlertService
import java.util.*

class TestActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var alertService: AlertService
    private lateinit var pikudHaorefApi: PikudHaorefApi
    private lateinit var resultTextView: TextView
    private lateinit var alertAdapter: AlertAdapter
    private lateinit var mediaPlayer: MediaPlayer
    private var googleMap: GoogleMap? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "TestActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        alertService = AlertService()
        pikudHaorefApi = PikudHaorefApi()
        mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound)

        resultTextView = findViewById(R.id.resultTextView)
        setupRecyclerView()
        setupMapFragment()

        findViewById<Button>(R.id.testAlertsButton).setOnClickListener {
            testAlerts()
        }

        findViewById<Button>(R.id.testLocationButton).setOnClickListener {
            testLocation()
        }

        findViewById<Button>(R.id.testApiButton).setOnClickListener {
            testPikudHaorefApi()
        }
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.alertsRecyclerView)
        alertAdapter = AlertAdapter()
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@TestActivity)
            adapter = alertAdapter
        }
    }

    private fun setupMapFragment() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { map ->
            googleMap = map
        }
    }

    private fun testAlerts() {
        if (checkLocationPermission()) {
            lifecycleScope.launch {
                try {
                    val location = fusedLocationClient.lastLocation.await()
                    if (location != null) {
                        val hasAlerts = alertService.checkForAlerts("test-group", location)
                        log("Alert check result: $hasAlerts")
                        if (hasAlerts) {
                            playAlertSound()
                        }
                    } else {
                        log("Location is null")
                    }
                } catch (e: Exception) {
                    log("Error checking alerts: ${e.message}")
                }
            }
        }
    }

    private fun testLocation() {
        if (checkLocationPermission()) {
            lifecycleScope.launch {
                try {
                    val location = fusedLocationClient.lastLocation.await()
                    if (location != null) {
                        log("Current location: ${location.latitude}, ${location.longitude}")
                        showLocationOnMap(location.latitude, location.longitude)
                    } else {
                        log("Location is null")
                    }
                } catch (e: Exception) {
                    log("Error getting location: ${e.message}")
                }
            }
        }
    }

    private fun testPikudHaorefApi() {
        lifecycleScope.launch {
            try {
                val alerts = pikudHaorefApi.getAlertHistory()
                val last24Hours = alerts.filter {
                    it.timestamp > System.currentTimeMillis() - 24 * 60 * 60 * 1000
                }
                log("Active alerts in last 24 hours: ${last24Hours.size}")
                alertAdapter.setAlerts(last24Hours)
            } catch (e: Exception) {
                log("Error getting alerts: ${e.message}")
            }
        }
    }

    private fun showLocationOnMap(latitude: Double, longitude: Double) {
        googleMap?.let { map ->
            val location = LatLng(latitude, longitude)
            map.clear()
            map.addMarker(MarkerOptions().position(location).title("Current Location"))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
            findViewById<View>(R.id.map).visibility = View.VISIBLE
        }
    }

    private fun playAlertSound() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val r = RingtoneManager.getRingtone(applicationContext, notification)
            r.play()
        } catch (e: Exception) {
            log("Error playing sound: ${e.message}")
        }
    }

    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        resultTextView.text = "${resultTextView.text}\n$message"
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}
