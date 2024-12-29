package nir.wolff.ui.test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import nir.wolff.R
import nir.wolff.api.PikudHaorefApi
import java.text.SimpleDateFormat
import java.util.*

class AlertAdapter : RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {
    private var alerts: List<PikudHaorefApi.Alert> = emptyList()

    fun setAlerts(newAlerts: List<PikudHaorefApi.Alert>) {
        alerts = newAlerts.sortedByDescending { it.timestamp }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(alerts[position])
    }

    override fun getItemCount(): Int = alerts.size

    class AlertViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleTextView: TextView = view.findViewById(R.id.alertTitleTextView)
        private val descriptionTextView: TextView = view.findViewById(R.id.alertDescriptionTextView)
        private val timeTextView: TextView = view.findViewById(R.id.alertTimeTextView)
        private val areaTextView: TextView = view.findViewById(R.id.alertAreaTextView)

        private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

        fun bind(alert: PikudHaorefApi.Alert) {
            titleTextView.text = alert.title
            descriptionTextView.text = alert.description
            timeTextView.text = dateFormat.format(Date(alert.timestamp))
            areaTextView.text = alert.area
        }
    }
}
