package nir.wolff.ui.alerts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import nir.wolff.R
import nir.wolff.api.PikudHaorefApi
import nir.wolff.databinding.ActivityAlertsBinding
import nir.wolff.service.AlertService

class AlertsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlertsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val auth = FirebaseAuth.getInstance()
    private val pikudHaorefApi = PikudHaorefApi()
    private val alertService = AlertService()
    private var alertCheckingJob: Job? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val ALERT_CHECK_INTERVAL = 30_000L // 30 seconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.alerts)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupViewPager()
        setupTestButton()
        
        // Start checking for alerts
        if (checkLocationPermission()) {
            startPeriodicAlertChecking()
        } else {
            requestLocationPermission()
        }
    }

    private fun setupViewPager() {
        val pagerAdapter = AlertsPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.current)
                1 -> getString(R.string.history)
                else -> ""
            }
        }.attach()
    }

    private fun setupTestButton() {
        binding.testAlertButton.setOnClickListener {
            if (checkLocationPermission()) {
                sendTestAlert()
            } else {
                requestLocationPermission()
            }
        }
    }

    private fun startPeriodicAlertChecking() {
        alertCheckingJob?.cancel()
        alertCheckingJob = lifecycleScope.launch {
            while (true) {
                checkForAlerts()
                delay(ALERT_CHECK_INTERVAL)
            }
        }
    }

    private suspend fun checkForAlerts() {
        try {
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
                // Get active alerts from Pikud Haoref
                val activeAlerts = pikudHaorefApi.getActiveAlerts()
                
                // Check if any alerts are relevant to current location
                val relevantAlerts = activeAlerts.filter { alert ->
                    pikudHaorefApi.isLocationInAlertArea(location, alert)
                }
                
                if (relevantAlerts.isNotEmpty()) {
                    // Show alert notification
                    val alertText = relevantAlerts.joinToString("\n") { it.description }
                    Toast.makeText(
                        this@AlertsActivity,
                        "⚠️ Alert: $alertText",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Refresh the current alerts fragment
                    (binding.viewPager.adapter as? AlertsPagerAdapter)?.refreshCurrentAlerts()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(
                this@AlertsActivity,
                "Error checking alerts: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun sendTestAlert() {
        lifecycleScope.launch {
            try {
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    // Get active alerts and check if we're in an alert area
                    val activeAlerts = pikudHaorefApi.getActiveAlerts()
                    val inAlertArea = activeAlerts.any { alert ->
                        pikudHaorefApi.isLocationInAlertArea(location, alert)
                    }
                    
                    Toast.makeText(
                        this@AlertsActivity,
                        if (inAlertArea) getString(R.string.in_alert_area)
                        else getString(R.string.no_active_alerts),
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Refresh the current alerts fragment
                    (binding.viewPager.adapter as? AlertsPagerAdapter)?.refreshCurrentAlerts()
                } else {
                    Toast.makeText(
                        this@AlertsActivity,
                        getString(R.string.location_not_available),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AlertsActivity,
                    getString(R.string.error_message, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startPeriodicAlertChecking()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.location_permission_required),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        alertCheckingJob?.cancel()
    }
}
