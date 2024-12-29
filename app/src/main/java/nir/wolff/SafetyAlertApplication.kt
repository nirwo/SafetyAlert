package nir.wolff

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.messaging.FirebaseMessaging

class SafetyAlertApplication : Application() {
    companion object {
        private const val TAG = "SafetyAlertApplication"
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Configure Firestore
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings

        // Initialize FCM
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d(TAG, "FCM Token: $token")
            
            // Store token in Firestore for the current user
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            auth.currentUser?.let { user ->
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d(TAG, "FCM token stored successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error storing FCM token", e)
                    }
            }
        }
    }
}
