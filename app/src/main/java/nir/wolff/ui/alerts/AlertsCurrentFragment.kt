package nir.wolff.ui.alerts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import nir.wolff.R
import nir.wolff.databinding.FragmentAlertsCurrentBinding
import nir.wolff.model.AlertEvent

class AlertsCurrentFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentAlertsCurrentBinding? = null
    private val binding get() = _binding!!
    private var map: GoogleMap? = null
    private var alertsListener: ListenerRegistration? = null
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertsCurrentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setupAlertsListener()
    }

    private fun setupAlertsListener() {
        alertsListener = firestore.collection("alerts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle error
                    return@addSnapshotListener
                }

                map?.clear()
                snapshot?.documents?.forEach { doc ->
                    val alert = AlertEvent.fromMap(doc.data ?: return@forEach, doc.id)
                    addAlertToMap(alert)
                }
            }
    }

    private fun addAlertToMap(alert: AlertEvent) {
        val location = LatLng(alert.location.latitude, alert.location.longitude)
        
        // Add marker
        map?.addMarker(
            MarkerOptions()
                .position(location)
                .title("Alert from ${alert.userEmail}")
        )

        // Add circle
        map?.addCircle(
            CircleOptions()
                .center(location)
                .radius(1000.0) // 1km radius
                .strokeColor(resources.getColor(R.color.warning_red, null))
                .fillColor(resources.getColor(R.color.warning_red_transparent, null))
        )

        // Move camera to the latest alert
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 12f))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        alertsListener?.remove()
        _binding = null
    }

    companion object {
        fun newInstance() = AlertsCurrentFragment()
    }
}
