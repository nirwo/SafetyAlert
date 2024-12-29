package nir.wolff.ui.alerts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import nir.wolff.databinding.ItemAlertBinding
import nir.wolff.model.AlertEvent
import java.text.SimpleDateFormat
import java.util.Locale

class AlertAdapter(
    private val onAlertClick: (AlertEvent) -> Unit
) : ListAdapter<AlertEvent, AlertAdapter.AlertViewHolder>(AlertDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemAlertBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AlertViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AlertViewHolder(
        private val binding: ItemAlertBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        fun bind(alert: AlertEvent) {
            binding.apply {
                alertTitleTextView.text = "Alert from ${alert.userEmail}"
                alertTimeTextView.text = dateFormat.format(alert.timestamp)
                alertAreaTextView.text = "Location: ${alert.location.latitude}, ${alert.location.longitude}"
                alertDescriptionTextView.text = alert.status

                root.setOnClickListener {
                    onAlertClick(alert)
                }
            }
        }
    }

    private class AlertDiffCallback : DiffUtil.ItemCallback<AlertEvent>() {
        override fun areItemsTheSame(oldItem: AlertEvent, newItem: AlertEvent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AlertEvent, newItem: AlertEvent): Boolean {
            return oldItem == newItem
        }
    }
}
