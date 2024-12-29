package nir.wolff.model

import java.io.Serializable

data class AlertEvent(
    val id: String = "",
    val groupId: String = "",
    val userEmail: String = "",
    val eventType: EventType = EventType.NEED_HELP,
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val location: GeoLocation = GeoLocation()
) {
    enum class EventType {
        NEED_HELP,
        ALL_CLEAR
    }

    val status: String
        get() = when (eventType) {
            EventType.NEED_HELP -> "Needs Help"
            EventType.ALL_CLEAR -> "Safe"
        }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "groupId" to groupId,
            "userEmail" to userEmail,
            "eventType" to eventType.name,
            "message" to message,
            "timestamp" to timestamp,
            "latitude" to location.latitude,
            "longitude" to location.longitude
        )
    }

    companion object {
        fun fromMap(data: Map<String, Any>, id: String): AlertEvent {
            return AlertEvent(
                id = id,
                groupId = data["groupId"] as? String ?: "",
                userEmail = data["userEmail"] as? String ?: "",
                eventType = try {
                    EventType.valueOf(data["eventType"] as? String ?: EventType.NEED_HELP.name)
                } catch (e: IllegalArgumentException) {
                    EventType.NEED_HELP
                },
                message = data["message"] as? String ?: "",
                timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                location = GeoLocation(
                    latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
                    longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0
                )
            )
        }
    }
}

data class GeoLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Serializable
