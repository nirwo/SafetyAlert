package nir.wolff.ui.alerts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import nir.wolff.databinding.FragmentAlertHistoryBinding
import nir.wolff.model.AlertEvent
import nir.wolff.network.AlertApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AlertHistoryFragment : Fragment() {
    private var _binding: FragmentAlertHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var alertAdapter: AlertAdapter
    private val auth = FirebaseAuth.getInstance()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://your-alert-api-url/") // Replace with your actual API URL
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val alertApi = retrofit.create(AlertApi::class.java)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        loadAlertHistory()
    }

    private fun setupRecyclerView() {
        alertAdapter = AlertAdapter { alert ->
            // Handle alert click if needed
        }

        binding.alertsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = alertAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadAlertHistory()
        }
    }

    private fun loadAlertHistory() {
        val userEmail = auth.currentUser?.email ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = alertApi.getAlertHistory(userEmail)
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful && response.body() != null) {
                    val alerts = response.body()!!
                    if (alerts.isEmpty()) {
                        binding.emptyView.visibility = View.VISIBLE
                    } else {
                        alertAdapter.submitList(alerts)
                    }
                } else {
                    showError("Failed to load alert history")
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                showError("Error: ${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = AlertHistoryFragment()
    }
}
