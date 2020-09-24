package com.kinikumuda.multikurir_driverapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kinikumuda.multikurir_driverapp.Model.DriverInfoModel
import java.lang.StringBuilder
import java.util.*

object Comon {
    fun buildWelcomMessage(): String {
        return StringBuilder("Welcome, ")
            .append(currentUser!!.firstName)
            .append(" ")
            .append(currentUser!!.lastName)
            .toString()
    }

    fun showNotification(context: Context, id: Int, title: String?, body: String?, intent: Intent?) {
        var pendingIntent:PendingIntent?=null
        if (intent !=null)
            pendingIntent=PendingIntent.getActivity(context,id,intent!!,PendingIntent.FLAG_UPDATE_CURRENT)
        val NOTIFICATION_CHANNEL_ID="com_kinikumuda_multikurir_driverapp"
        val notificationManager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            val notificationChannel=NotificationChannel(NOTIFICATION_CHANNEL_ID,"Multi Kurir",
            NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.description="Multi Kurir"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor= Color.RED
            notificationChannel.vibrationPattern= longArrayOf(0,1000,500,1000)
            notificationChannel.enableVibration(true)

            notificationManager.createNotificationChannel(notificationChannel)

        }
        val builder=NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID)
        builder.setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setSmallIcon(R.drawable.ic_baseline_directions_car_24)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources,R.drawable.ic_baseline_directions_car_24))
        if (pendingIntent != null)
            builder.setContentIntent(pendingIntent!!)
        val notification=builder.build()
        notificationManager.notify(id,notification)

    }

    val NOTI_BODY: String="body"
    val NOTI_TITLE: String="title"
    
    val TOKEN_REFERENCE: String = "Token"
    val DRIVER_LOCATION_REFERENCE: String="DriverLocation"
    var currentUser: DriverInfoModel?=null
    val DRIVER_INFO_REFERENCE: String="DriverInfo"
}