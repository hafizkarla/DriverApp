package com.kinikumuda.multikurir_driverapp.Services

import com.kinikumuda.multikurir_driverapp.Comon
import com.kinikumuda.multikurir_driverapp.Utils.UserUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService: FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (FirebaseAuth.getInstance().currentUser !=null)
            UserUtils.updateToken(this,token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data=remoteMessage.data
        if (data!=null)
        {
            Comon.showNotification(this, Random.nextInt(),
            data[Comon.NOTI_TITLE],
            data[Comon.NOTI_BODY],
            null)
        }
    }
}