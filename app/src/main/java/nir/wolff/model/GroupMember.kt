package nir.wolff.model

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import java.io.Serializable

data class GroupMember(
    val email: String,
    val name: String = "",
    val status: Status = Status.UNKNOWN,
    val lastLocation: GeoLocation? = null
) : Serializable {
    constructor() : this("", "", Status.UNKNOWN, null)

    data class GeoLocation(
        val latitude: Double = 0.0,
        val longitude: Double = 0.0
    ) {
        fun toLatLng(): LatLng = LatLng(latitude, longitude)
        
        companion object {
            fun fromMap(map: Map<String, Any>): GeoLocation {
                return GeoLocation(
                    latitude = (map["latitude"] as? Number)?.toDouble() ?: 0.0,
                    longitude = (map["longitude"] as? Number)?.toDouble() ?: 0.0
                )
            }
        }
    }

    enum class Status {
        SAFE, UNSAFE, UNKNOWN
    }

    companion object {
        private const val TAG = "GroupMember"

        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any>): GroupMember {
            Log.d(TAG, "Converting map to GroupMember: $map")
            val locationMap = map["lastLocation"] as? Map<String, Any>
            val location = locationMap?.let { GeoLocation.fromMap(it) }
            
            return GroupMember(
                email = (map["email"] as? String ?: "").also { 
                    if (it.isEmpty()) Log.w(TAG, "Empty email in map: $map") 
                },
                name = (map["name"] as? String ?: "").also { 
                    if (it.isEmpty()) Log.w(TAG, "Empty name in map: $map") 
                },
                status = try {
                    Status.valueOf((map["status"] as? String)?.uppercase() ?: Status.UNKNOWN.name)
                } catch (e: IllegalArgumentException) {
                    Status.UNKNOWN
                },
                lastLocation = location
            ).also {
                Log.d(TAG, "Created GroupMember object: $it")
            }
        }
    }

    fun toMap(): Map<String, Any> {
        Log.d(TAG, "Converting member to map: $this")
        val map = mutableMapOf<String, Any>(
            "email" to email,
            "name" to name,
            "status" to status.name
        )
        
        lastLocation?.let {
            map["lastLocation"] = mapOf(
                "latitude" to it.latitude,
                "longitude" to it.longitude
            )
        }
        
        return map.also {
            Log.d(TAG, "Converted map: $it")
        }
    }

    fun isSafe(): Boolean = status == Status.SAFE
    fun isUnsafe(): Boolean = status == Status.UNSAFE
    fun needsHelp(): Boolean = status == Status.UNSAFE
}
