package nir.wolff.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nir.wolff.repository.UserRepository
import nir.wolff.util.NotificationHelper

class AlertFirebaseMessagingService : FirebaseMessagingService() {
    private val userRepository = UserRepository()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        remoteMessage.data.let { data ->
            val title = data["title"]
            val message = data["message"]
            val groupId = data["groupId"]
            
            if (title != null && message != null) {
                NotificationHelper.showStatusUpdateNotification(
                    context = this,
                    title = title,
                    message = message,
                    groupId = groupId
                )
            }
        }
    }

    override fun onNewToken(token: String) {
        scope.launch {
            try {
                userRepository.updateFcmToken(token)
            } catch (e: Exception) {
                // Handle token update error
            }
        }
    }
}
