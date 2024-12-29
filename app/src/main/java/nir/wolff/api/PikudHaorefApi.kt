package nir.wolff.api

import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.math.abs

class PikudHaorefApi {
    companion object {
        private const val BASE_URL = "https://www.oref.org.il/WarningMessages/alert/alerts.json"
        private const val HISTORY_URL = "https://www.oref.org.il/WarningMessages/History/AlertsHistory.json"
        
        // Districts mapping with their approximate coordinates (latitude, longitude)
        private val DISTRICTS = mapOf(
            "תל אביב - מרכז העיר" to Coordinates(32.0853, 34.7818),
            "תל אביב - צפון העיר" to Coordinates(32.1133, 34.7844),
            "תל אביב - דרום העיר" to Coordinates(32.0504, 34.7666),
            "ירושלים - מרכז העיר" to Coordinates(31.7683, 35.2137),
            "ירושלים - דרום העיר" to Coordinates(31.7500, 35.2167),
            "ירושלים - צפון העיר" to Coordinates(31.7900, 35.2200),
            "חיפה - מערב העיר" to Coordinates(32.8191, 34.9983),
            "חיפה - מזרח העיר" to Coordinates(32.8191, 35.0200)
        )

        private const val MAX_DISTANCE_KM = 10.0 // Maximum distance in kilometers to consider location in district
    }

    suspend fun getActiveAlerts(): List<Alert> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(BASE_URL).openConnection() as HttpsURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("X-Requested-With", "XMLHttpRequest")
                setRequestProperty("Referer", "https://www.oref.org.il/")
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            parseAlerts(response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAlertHistory(): List<Alert> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(HISTORY_URL).openConnection() as HttpsURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("X-Requested-With", "XMLHttpRequest")
                setRequestProperty("Referer", "https://www.oref.org.il/")
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            parseAlerts(response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseAlerts(jsonString: String): List<Alert> {
        val alerts = mutableListOf<Alert>()
        try {
            val json = JSONObject(jsonString)
            val alertsArray = json.optJSONArray("data") ?: return emptyList()

            for (i in 0 until alertsArray.length()) {
                val alertObj = alertsArray.getJSONObject(i)
                alerts.add(
                    Alert(
                        id = alertObj.optString("alertId", ""),
                        title = alertObj.optString("title", ""),
                        description = alertObj.optString("desc", ""),
                        area = alertObj.optString("data", ""),
                        timestamp = alertObj.optLong("alertDate", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return alerts
    }

    fun isLocationInAlertArea(location: Location, alert: Alert): Boolean {
        val district = findDistrictForLocation(location)
        return alert.area.contains(district, ignoreCase = true)
    }

    private fun findDistrictForLocation(location: Location): String {
        var closestDistrict = ""
        var minDistance = Double.MAX_VALUE

        DISTRICTS.forEach { (district, coordinates) ->
            val distance = calculateDistance(
                location.latitude, location.longitude,
                coordinates.latitude, coordinates.longitude
            )
            if (distance < minDistance && distance <= MAX_DISTANCE_KM) {
                minDistance = distance
                closestDistrict = district
            }
        }

        return closestDistrict
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Earth's radius in kilometers

        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)

        val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return R * c
    }

    data class Alert(
        val id: String,
        val title: String,
        val description: String,
        val area: String,
        val timestamp: Long
    )

    private data class Coordinates(
        val latitude: Double,
        val longitude: Double
    )
}
