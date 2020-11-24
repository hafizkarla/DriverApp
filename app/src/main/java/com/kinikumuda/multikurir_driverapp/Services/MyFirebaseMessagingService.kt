package com.kinikumuda.multikurir_driverapp.Services

import android.content.SharedPreferences
import com.kinikumuda.multikurir_driverapp.Comon
import com.kinikumuda.multikurir_driverapp.Utils.UserUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kinikumuda.multikurir_driverapp.Model.EventBus.DriverRequestReceived
import org.greenrobot.eventbus.EventBus
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
            if (data[Comon.NOTI_TITLE].equals(Comon.REQUEST_DRIVER_TITLE))
            {
                val driverRequestReceived=DriverRequestReceived()
                driverRequestReceived.key=data[Comon.RIDER_KEY]
                driverRequestReceived.pickupLocation=data[Comon.PICKUP_LOCATION]
                driverRequestReceived.pickupLocationString=data[Comon.PICKUP_LOCATION_STRING]
                driverRequestReceived.destinationLocation=data[Comon.DESTINATION_LOCATION]
                driverRequestReceived.destinationLocationString=data[Comon.DESTINATION_LOCATION_STRING]
                driverRequestReceived.typeOrder=data[Comon.TYPE_ORDER]

                EventBus.getDefault().postSticky(driverRequestReceived)
            }
            else
            {
                Comon.showNotification(this, Random.nextInt(),
                    data[Comon.NOTI_TITLE],
                    data[Comon.NOTI_BODY],
                    null)
            }

        }
    }

}