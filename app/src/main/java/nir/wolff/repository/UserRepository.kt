package nir.wolff.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "UserRepository"
    }

    suspend fun createOrUpdateUser(email: String, name: String? = null) {
        Log.d(TAG, "Creating/updating user with email: $email")
        auth.currentUser?.uid?.let { userId ->
            val userData = hashMapOf(
                "email" to email,
                "name" to (name ?: email.substringBefore('@')),
                "updatedAt" to System.currentTimeMillis()
            )

            Log.d(TAG, "Saving user data: $userData")
            firestore.collection("users")
                .document(userId)
                .set(userData)
                .await()
            Log.d(TAG, "User data saved successfully")
        }
    }

    suspend fun getUsersByEmail(emails: List<String>): List<String> {
        Log.d(TAG, "Getting users by emails: $emails")
        val userIds = mutableListOf<String>()
        
        for (email in emails) {
            Log.d(TAG, "Querying for email: $email")
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val userId = querySnapshot.documents[0].id
                Log.d(TAG, "Found user ID: $userId for email: $email")
                userIds.add(userId)
            } else {
                Log.w(TAG, "No user found for email: $email")
            }
        }
        
        Log.d(TAG, "Returning user IDs: $userIds")
        return userIds
    }

    suspend fun updateFcmToken(token: String) {
        Log.d(TAG, "Updating FCM token: $token")
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users")
                .document(userId)
                .update("fcmToken", token)
                .await()
            Log.d(TAG, "FCM token updated successfully")
        }
    }
}
