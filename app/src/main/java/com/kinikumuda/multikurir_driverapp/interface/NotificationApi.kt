package com.kinikumuda.multikurir_driverapp.`interface`

import com.kinikumuda.multikurir_driverapp.Constrants.Constants.Companion.CONTENT_TYPE
import com.kinikumuda.multikurir_driverapp.Constrants.Constants.Companion.SERVER_KEY
import com.kinikumuda.multikurir_driverapp.Model.PushNotification
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface NotificationApi {

    @Headers("Authorization: key=$SERVER_KEY","Content-type:$CONTENT_TYPE")
    @POST("fcm/send")
    suspend fun postNotification(
        @Body notification: PushNotification
    ): Response<ResponseBody>
}