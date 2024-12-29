package nir.wolff

import android.app.Application
import com.google.firebase.FirebaseApp
import nir.wolff.util.NotificationHelper

class SafetyAlertApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        NotificationHelper.createNotificationChannel(this)
    }
}
