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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import nir.wolff.R
import nir.wolff.databinding.ActivityAlertsBinding
import nir.wolff.network.AlertApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AlertsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlertsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val auth = FirebaseAuth.getInstance()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://your-alert-api-url/") // Replace with your actual API URL
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val alertApi = retrofit.create(AlertApi::class.java)

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Alerts"

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupViewPager()
        setupTestButton()
    }

    private fun setupViewPager() {
        val pagerAdapter = AlertsPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Current"
                1 -> "History"
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

    private fun sendTestAlert() {
        lifecycleScope.launch {
            try {
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    val userEmail = auth.currentUser?.email
                    if (userEmail != null) {
                        val response = alertApi.testAlert(
                            userEmail = userEmail,
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                        
                        if (response.isSuccessful) {
                            Toast.makeText(
                                this@AlertsActivity,
                                "Test alert sent successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@AlertsActivity,
                                "Failed to send test alert",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(
                        this@AlertsActivity,
                        "Location not available",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AlertsActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendTestAlert()
            } else {
                Toast.makeText(
                    this,
                    "Location permission required to send test alerts",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
