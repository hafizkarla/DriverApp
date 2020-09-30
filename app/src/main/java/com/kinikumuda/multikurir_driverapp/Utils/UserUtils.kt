package com.kinikumuda.multikurir_driverapp.Utils

import android.app.Activity
import android.content.Context
import android.provider.Settings.Global.getString
import android.provider.Settings.Secure.getString
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.kinikumuda.multikurir_driverapp.Comon
import com.kinikumuda.multikurir_driverapp.Model.FCMSendData
import com.kinikumuda.multikurir_driverapp.Model.TokenModel
import com.kinikumuda.multikurir_driverapp.R
import com.kinikumuda.multikurir_driverapp.Remote.IFCMService
import com.kinikumuda.multikurir_driverapp.Remote.RetrofitFCMClient
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

object UserUtils {
    fun updateUser(
        view: View?,
        updateData:Map<String,Any>
    ){
        FirebaseDatabase.getInstance()
            .getReference(Comon.DRIVER_INFO_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .updateChildren(updateData)
            .addOnFailureListener{e->
                Snackbar.make(view!!,e.message!!,Snackbar.LENGTH_SHORT).show()
            }.addOnSuccessListener {
                Snackbar.make(view!!,"Update information success",Snackbar.LENGTH_SHORT).show()
            }
    }

    fun updateToken(context: Context, token: String) {
        val tokenModel= TokenModel()
        tokenModel.token=token;

        FirebaseDatabase.getInstance()
            .getReference(Comon.TOKEN_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .setValue(tokenModel)
            .addOnFailureListener{e-> Toast.makeText(context,e.message, Toast.LENGTH_SHORT).show()}
            .addOnSuccessListener {  }
    }

    fun sendDeclineRequest(view: View, activity: Activity, key: String) {
        val compositeDisposable= CompositeDisposable()
        val ifcmService= RetrofitFCMClient.instance!!.create(IFCMService::class.java)

        //get token
        FirebaseDatabase.getInstance()
            .getReference(Comon.TOKEN_REFERENCE)
            .child(key)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists())
                    {
                        val tokenModel=dataSnapshot.getValue(TokenModel::class.java)

                        val notificationData:MutableMap<String,String> = HashMap()
                        notificationData.put(Comon.NOTI_TITLE,Comon.REQUEST_DRIVER_DECLINE)
                        notificationData.put(Comon.NOTI_BODY,"This message represent for decline action from Driver")
                        notificationData.put(Comon.DRIVER_KEY,FirebaseAuth.getInstance().currentUser!!.uid)


                        val fcmSendData= FCMSendData(tokenModel!!.token,notificationData)

                        compositeDisposable.add(ifcmService.sendNotification(fcmSendData)!!
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({fcmResponse->
                                if (fcmResponse!!.success == 0)
                                {
                                    compositeDisposable.clear()
                                    Snackbar.make(view,activity.getString(R.string.decline_failed),Snackbar.LENGTH_LONG).show()
                                }
                                else
                                {
                                    Snackbar.make(view,activity.getString(R.string.decline_success),Snackbar.LENGTH_LONG).show()
                                }
                            },{t: Throwable?->

                                compositeDisposable.clear()
                                Snackbar.make(view,t!!.message!!,Snackbar.LENGTH_LONG).show()

                            }))
                    }
                    else
                    {
                        compositeDisposable.clear()
                        Snackbar.make(view,activity.getString(R.string.token_not_found),Snackbar.LENGTH_LONG).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Snackbar.make(view,databaseError.message,Snackbar.LENGTH_LONG).show()
                }

            })
    }
    fun sendDone(view: View, activity: Activity, key: String) {
        val compositeDisposable= CompositeDisposable()
        val ifcmService= RetrofitFCMClient.instance!!.create(IFCMService::class.java)

        //get token
        FirebaseDatabase.getInstance()
            .getReference(Comon.TOKEN_REFERENCE)
            .child(key)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists())
                    {
                        val tokenModel=dataSnapshot.getValue(TokenModel::class.java)

                        val notificationData:MutableMap<String,String> = HashMap()
                        notificationData.put(Comon.NOTI_TITLE,Comon.REQUEST_DRIVER_DONE)
                        notificationData.put(Comon.NOTI_BODY,"Your package has been arrived!")
                        notificationData.put(Comon.DRIVER_KEY,FirebaseAuth.getInstance().currentUser!!.uid)


                        val fcmSendData= FCMSendData(tokenModel!!.token,notificationData)

                        compositeDisposable.add(ifcmService.sendNotification(fcmSendData)!!
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({fcmResponse->
                                if (fcmResponse!!.success == 0)
                                {
                                    compositeDisposable.clear()
                                    Snackbar.make(view,activity.getString(R.string.done_request_failed),Snackbar.LENGTH_LONG).show()
                                }
                                else
                                {
                                    Snackbar.make(view,activity.getString(R.string.done_success),Snackbar.LENGTH_LONG).show()
                                }
                            },{t: Throwable?->

                                compositeDisposable.clear()
                                Snackbar.make(view,t!!.message!!,Snackbar.LENGTH_LONG).show()

                            }))
                    }
                    else
                    {
                        compositeDisposable.clear()
                        Snackbar.make(view,activity.getString(R.string.token_not_found),Snackbar.LENGTH_LONG).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Snackbar.make(view,databaseError.message,Snackbar.LENGTH_LONG).show()
                }

            })
    }

    fun sendAcceptRequestToRider(view: View?, requireContext: Context, key: String, tripNumberId: String?) {
        val compositeDisposable=CompositeDisposable()
        val ifcmService=RetrofitFCMClient.instance!!.create(IFCMService::class.java)

        //get token
        FirebaseDatabase.getInstance()
            .getReference(Comon.TOKEN_REFERENCE)
            .child(key)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists())
                    {
                        val tokenModel=dataSnapshot.getValue(TokenModel::class.java)

                        val notificationData:MutableMap<String,String> = HashMap()
                        notificationData.put(Comon.NOTI_TITLE,Comon.REQUEST_DRIVER_ACCEPT)
                        notificationData.put(Comon.NOTI_BODY,"This message represent for accept action from Driver")
                        notificationData.put(Comon.DRIVER_KEY,FirebaseAuth.getInstance().currentUser!!.uid)
                        notificationData.put(Comon.TRIP_KEY,tripNumberId!!)


                        val fcmSendData= FCMSendData(tokenModel!!.token,notificationData)

                        compositeDisposable.add(ifcmService.sendNotification(fcmSendData)!!
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({fcmResponse->
                                if (fcmResponse!!.success == 0)
                                {
                                    compositeDisposable.clear()
                                    Snackbar.make(view!!,requireContext.getString(R.string.accept_failed),Snackbar.LENGTH_LONG).show()
                                }

                            },{t: Throwable?->

                                compositeDisposable.clear()
                                Snackbar.make(view!!,t!!.message!!,Snackbar.LENGTH_LONG).show()

                            }))
                    }
                    else
                    {
                        compositeDisposable.clear()
                        Snackbar.make(view!!,requireContext.getString(R.string.token_not_found),Snackbar.LENGTH_LONG).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Snackbar.make(view!!,databaseError.message,Snackbar.LENGTH_LONG).show()
                }

            })
    }
}