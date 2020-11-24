package com.kinikumuda.multikurir_driverapp.Model

import com.google.firebase.auth.FirebaseAuth

class Message {
    constructor() //empty for firebase

    constructor(messageText: String){
        text = messageText
    }
    var text: String? = null
    var timestamp: Long = System.currentTimeMillis()
    var sender:String = FirebaseAuth.getInstance().currentUser!!.uid
}