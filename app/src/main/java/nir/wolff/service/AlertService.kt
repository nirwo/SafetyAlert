package nir.wolff.service

import android.location.Location
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import nir.wolff.api.PikudHaorefApi

class AlertService {
    private val firestore = FirebaseFirestore.getInstance()
    private val pikudHaorefApi = PikudHaorefApi()

    suspend fun checkForAlerts(groupId: String, location: Location): Boolean {
        return try {
            // Check for active alerts from Pikud HaOref
            val activeAlerts = pikudHaorefApi.getActiveAlerts()
            
            // If there are any alerts that affect the current location
            val hasRelevantAlerts = activeAlerts.any { alert ->
                pikudHaorefApi.isLocationInAlertArea(location, alert)
            }

            if (hasRelevantAlerts) {
                // Store the alert in Firestore for the group
                val alertsRef = firestore.collection("groups").document(groupId)
                    .collection("alerts")
                
                activeAlerts.forEach { alert ->
                    alertsRef.document(alert.id).set(
                        hashMapOf(
                            "title" to alert.title,
                            "description" to alert.description,
                            "area" to alert.area,
                            "timestamp" to alert.timestamp
                        )
                    ).await()
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun markUserStatus(userId: String, groupId: String, isSafe: Boolean) {
        try {
            val userRef = firestore.collection("groups").document(groupId)
                .collection("members").document(userId)
            
            userRef.update(mapOf(
                "isSafe" to isSafe,
                "lastUpdated" to System.currentTimeMillis()
            )).await()
            
            // Get user's name for notification
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userName = userDoc.getString("name") ?: "Unknown User"
            
            // Create status update notification for group members
            val groupDoc = firestore.collection("groups").document(groupId).get().await()
            val membersData = groupDoc.get("members")
            val members = if (membersData is List<*>) {
                membersData.filterIsInstance<String>()
            } else {
                emptyList()
            }
            
            for (memberId in members) {
                if (memberId != userId) {
                    val notification = hashMapOf(
                        "userId" to memberId,
                        "title" to "Status Update",
                        "message" to "$userName is ${if (isSafe) "safe" else "needs help"}",
                        "timestamp" to System.currentTimeMillis()
                    )
                    
                    firestore.collection("notifications")
                        .add(notification)
                        .await()
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }
}
