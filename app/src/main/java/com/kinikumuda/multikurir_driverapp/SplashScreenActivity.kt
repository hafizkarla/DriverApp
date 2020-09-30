package com.kinikumuda.multikurir_driverapp

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.auth.api.credentials.IdentityProviders
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.kinikumuda.multikurir_driverapp.Model.DriverInfoModel
import com.kinikumuda.multikurir_driverapp.Utils.UserUtils
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_splash_screen.*
import java.util.*
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {

    companion object{
        private val LOGIN_REQUEST_CODE=7171
    }
    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener

    private lateinit var database: FirebaseDatabase
    private lateinit var driverInfoRef: DatabaseReference


    override fun onStart() {
        super.onStart()
        delaySplashScreen()
    }

    override fun onStop() {
        if (firebaseAuth!=null&&listener!=null) firebaseAuth.removeAuthStateListener(listener)
        super.onStop()
    }
    private fun delaySplashScreen() {
        Completable.timer(3,TimeUnit.SECONDS,AndroidSchedulers.mainThread())
            .subscribe({
                firebaseAuth.addAuthStateListener(listener)
            })
    }
    private fun showLoginLayout() {
        val authMethodPickerLayout= AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
            .setPhoneButtonId(R.id.btn_phone_sign_in)
            .setGoogleButtonId(R.id.btn_google_sign_in)
            .build();

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setTheme(R.style.LoginTheme)
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build()
            , LOGIN_REQUEST_CODE)
    }
    private fun init() {

        database= FirebaseDatabase.getInstance()
        driverInfoRef=database.getReference(Comon.DRIVER_INFO_REFERENCE)


        providers= Arrays.asList(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        firebaseAuth=FirebaseAuth.getInstance()
        listener=FirebaseAuth.AuthStateListener { myFirebaseAuth ->
            val user=myFirebaseAuth.currentUser
            if(user!=null)
            {

                FirebaseInstanceId.getInstance()
                    .instanceId
                    .addOnFailureListener{e->Toast.makeText(this@SplashScreenActivity,e.message,Toast.LENGTH_SHORT).show()}
                    .addOnSuccessListener { instanceIdResult->

                        Log.d("TOKEN",instanceIdResult.token)
                        UserUtils.updateToken(this@SplashScreenActivity,instanceIdResult.token)


                    }
                checkUserFromFirebase()
            }
            else
                showLoginLayout()
        }
    }

    private fun checkUserFromFirebase() {

        driverInfoRef
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    Toast.makeText(this@SplashScreenActivity,p0.message,Toast.LENGTH_SHORT).show()
                }
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists())
                    {
                        //Toast.makeText(this@SplashScreenActivity,"User already registered.",Toast.LENGTH_SHORT).show()
                        val model=dataSnapshot.getValue(DriverInfoModel::class.java)
                        goToHomeActivity(model)
                    }
                    else{
                        showRegisterLayout()
                    }
                }




            })
    }

    private fun goToHomeActivity(model: DriverInfoModel?) {
        Comon.currentUser=model
        startActivity(Intent(this,DriverHomeActivity::class.java))
        finish()
    }

    private fun showRegisterLayout() {
        val builder = AlertDialog.Builder(this, R.style.DialogTheme)
        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_register,null)

        val edt_first_name = itemView.findViewById<View>(R.id.edt_first_name) as TextInputEditText
        val edt_last_name = itemView.findViewById<View>(R.id.edt_last_name) as TextInputEditText
        val edt_phone_number = itemView.findViewById<View>(R.id.edt_phone_number) as TextInputEditText
        val edt_id_number = itemView.findViewById<View>(R.id.edt_id_number) as TextInputEditText
        val edt_drive_license = itemView.findViewById<View>(R.id.edt_drive_license) as TextInputEditText
        val edt_motor_type = itemView.findViewById<View>(R.id.edt_motor_type) as TextInputEditText
        val edt_vehicle_license = itemView.findViewById<View>(R.id.edt_vehicle_license) as TextInputEditText

        val btn_continue = itemView.findViewById<View>(R.id.btn_register) as Button

        //set data
        if (FirebaseAuth.getInstance().currentUser!!.phoneNumber != null &&
            !TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser!!.phoneNumber))
            edt_phone_number.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)

        //view
        builder.setView(itemView)
        val dialog = builder.create()
        dialog.show()

        //event
        btn_continue.setOnClickListener {
            if(TextUtils.isDigitsOnly(edt_first_name.text.toString())){
                Toast.makeText(this@SplashScreenActivity,"Please enter First Name",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else if(TextUtils.isDigitsOnly(edt_last_name.text.toString())){
                Toast.makeText(this@SplashScreenActivity,"Please enter Last Name",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else if(TextUtils.isDigitsOnly(edt_phone_number.text.toString())){
                Toast.makeText(this@SplashScreenActivity,"Please enter Phone Number",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else if(TextUtils.isEmpty(edt_id_number.text.toString())){
                Toast.makeText(this@SplashScreenActivity,"Please enter Id Number",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else if(TextUtils.isEmpty(edt_drive_license.text.toString())){
                Toast.makeText(this@SplashScreenActivity,"Please enter Drive License",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else if(TextUtils.isDigitsOnly(edt_motor_type.text.toString())){
                Toast.makeText(this@SplashScreenActivity,"Please enter Motorcycle Type",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else if(TextUtils.isDigitsOnly(edt_vehicle_license.text.toString())){
                Toast.makeText(this@SplashScreenActivity,"Please enter Vehicle License Number",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            else{
                val model= DriverInfoModel()
                model.firstName=edt_first_name.text.toString()
                model.lastName=edt_last_name.text.toString()
                model.phoneNumber=edt_phone_number.text.toString()
                model.rating=5.0

                //additional
                model.idNumber=edt_id_number.text.toString()
                model.motorType=edt_motor_type.text.toString()
                model.driveLicense=edt_drive_license.text.toString()
                model.vehicleLicenseNumber=edt_vehicle_license.text.toString()


                driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                    .setValue(model)
                    .addOnFailureListener{e->
                        Toast.makeText(this@SplashScreenActivity,""+e.message,Toast.LENGTH_SHORT).show()
                        dialog.dismiss()

                        progress_bar.visibility= View.GONE
                    }
                    .addOnSuccessListener {
                        Toast.makeText(this@SplashScreenActivity,"Register successfully.",Toast.LENGTH_SHORT).show()
                        dialog.dismiss()

                        goToHomeActivity(model)

                        progress_bar.visibility= View.GONE
                    }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        init()



    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode== LOGIN_REQUEST_CODE) {
            val response= IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                val user=FirebaseAuth.getInstance().currentUser
            }
            else
                Toast.makeText(this@SplashScreenActivity,""+response!!.error!!.message,Toast.LENGTH_SHORT).show()
        }

    }

}