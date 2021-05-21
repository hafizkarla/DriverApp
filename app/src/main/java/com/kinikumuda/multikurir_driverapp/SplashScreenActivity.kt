package com.kinikumuda.multikurir_driverapp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.kinikumuda.multikurir_driverapp.Model.DriverInfoModel
import com.kinikumuda.multikurir_driverapp.Utils.UserUtils
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_splash_screen.*
import kotlinx.android.synthetic.main.layout_register.*
import java.util.*
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    companion object{
        private val LOGIN_REQUEST_CODE = 7171
    }

    lateinit var providers: List<AuthUI.IdpConfig>
    lateinit var firebaseAuth: FirebaseAuth
    lateinit var listener: FirebaseAuth.AuthStateListener

    private lateinit var database: FirebaseDatabase
    private lateinit var driverInfoRef: DatabaseReference


    override fun onStart() {
        super.onStart()
        delaySplashScree()

    }

    @SuppressLint("CheckResult")
    private fun delaySplashScree() {
        Completable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribe {
                firebaseAuth.addAuthStateListener(listener)
            }
    }

    override fun onStop() {
        if (firebaseAuth!=null && listener!=null)firebaseAuth.removeAuthStateListener(listener)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        auth = FirebaseAuth.getInstance()

        // Restore instance state
        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState)
        }

        providers = listOf(
            AuthUI.IdpConfig.PhoneBuilder().build()
        )

        init()
    }

    private fun init() {
        database= FirebaseDatabase.getInstance()
        driverInfoRef=database.getReference(Comon.DRIVER_INFO_REFERENCE)
        providers= listOf(
            AuthUI.IdpConfig.PhoneBuilder().build()
        )
        firebaseAuth= FirebaseAuth.getInstance()
        listener= FirebaseAuth.AuthStateListener { myFirebaseAuth->
            val user = myFirebaseAuth.currentUser
            if (user!=null)
            {
                FirebaseInstanceId.getInstance()
                    .instanceId
                    .addOnFailureListener{e->
                        Toast.makeText(this@SplashScreenActivity,e.message,
                            Toast.LENGTH_SHORT).show()}
                    .addOnSuccessListener { instanceIdResult->

                        Log.d("TOKEN",instanceIdResult.token)
                        UserUtils.updateToken(this@SplashScreenActivity,instanceIdResult.token)


                    }
                checkUserFromFirebase()
            }
            else{
                showLoginLayout()
            }
        }
    }

//    private fun showSignInOptions() {
//        startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder()
//            .setAvailableProviders(providers)
//            .build(), LOGIN_REQUEEST_CODE)
//    }

    private fun showLoginLayout() {
        val authMethodPickerLayout= AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
            .setPhoneButtonId(R.id.btn_phone_sign_in)
            .build()

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setTheme(R.style.LoginTheme)
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build(), LOGIN_REQUEST_CODE


        )

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode== LOGIN_REQUEST_CODE){
            val response= IdpResponse.fromResultIntent(data)
            if (resultCode== Activity.RESULT_OK){
                val user= FirebaseAuth.getInstance().currentUser
            }
            else
                Toast.makeText(this@SplashScreenActivity,response!!.error!!.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun checkUserFromFirebase() {
        driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists()){
                        val model=p0.getValue(DriverInfoModel::class.java)
                        goToHomeActivity(model)
//                        if (model!!.verification) {
//                            goToHomeActivity(model)
//                        }
//                        else{
//                            goToVerifyActivity()
//                        }
                    }
                    else{
                        showRegisterLayout()
                    }
                }

                override fun onCancelled(p0: DatabaseError) {
                    Toast.makeText(this@SplashScreenActivity,p0.message, Toast.LENGTH_LONG).show()
                }


            })
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
        val edt_address = itemView.findViewById<View>(R.id.edt_address) as TextInputEditText
        val edt_agama = itemView.findViewById<View>(R.id.edt_agama) as TextInputEditText
        val edt_asal = itemView.findViewById<View>(R.id.edt_asal) as TextInputEditText
        val edt_jenis_kelamin = itemView.findViewById<View>(R.id.edt_jenis_kelamin) as TextInputEditText
        val edt_status = itemView.findViewById<View>(R.id.edt_status) as TextInputEditText
        val tiEditDate = itemView.findViewById<View>(R.id.tiEditDate) as TextInputLayout
        val edt_tanggal_lahir = itemView.findViewById<View>(R.id.edt_tanggal_lahir) as TextInputEditText

        val btn_continue = itemView.findViewById<View>(R.id.btn_register) as Button

        val radiogroup=itemView.findViewById<View>(R.id.radiogroup) as RadioGroup
        val motor =itemView.findViewById<View>(R.id.motor) as RadioButton
        val mobil =itemView.findViewById<View>(R.id.mobil) as RadioButton

        //set data
        if (FirebaseAuth.getInstance().currentUser!!.phoneNumber != null &&
            !TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser!!.phoneNumber))
            edt_phone_number.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)

        //view
        builder.setView(itemView)
        val dialog = builder.create()
        dialog.show()

        tiEditDate.setEndIconOnClickListener {
            val c = Calendar.getInstance()
            val day = c.get(Calendar.DAY_OF_MONTH)
            val month = c.get(Calendar.MONTH)
            val year = c.get(Calendar.YEAR)

            val dpd = DatePickerDialog(
                this,
                DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                    if ((month + 1).toString().length==2) {
                        edt_tanggal_lahir.setText("$year-${month+1}-$dayOfMonth")
                    }else{
                        edt_tanggal_lahir.setText("$year-0${month+1}-$dayOfMonth")
                    }
                },
                year,
                month,
                day
            )
            dpd.show()
        }

        //event
        btn_continue.setOnClickListener {
            when {
                TextUtils.isEmpty(edt_first_name.text.toString()) -> {
                    Toast.makeText(this@SplashScreenActivity,"Masukkan Nama Depan", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                TextUtils.isEmpty(edt_last_name.text.toString()) -> {
                    Toast.makeText(this@SplashScreenActivity,"Masukkan Nama Belakang", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                TextUtils.isEmpty(edt_phone_number.text.toString()) -> {
                    Toast.makeText(this@SplashScreenActivity,"Masukkan Nomor Telepon", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                TextUtils.isEmpty(edt_id_number.text.toString()) -> {
                    Toast.makeText(this@SplashScreenActivity,"Masukkan Nomor KTP", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                TextUtils.isEmpty(edt_drive_license.text.toString()) -> {
                    Toast.makeText(this@SplashScreenActivity,"Masukkan Lisensi Mengemudi", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                TextUtils.isEmpty(edt_motor_type.text.toString()) -> {
                    Toast.makeText(this@SplashScreenActivity,"Masukkan Jenis Motor", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                TextUtils.isEmpty(edt_vehicle_license.text.toString()) -> {
                    Toast.makeText(this@SplashScreenActivity,"Masukkan Nomor Polisi Kendaraan", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                TextUtils.isEmpty(edt_address.text.toString()) -> {
                    Toast.makeText(this@SplashScreenActivity,"Masukkan Alamat", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                TextUtils.isEmpty(edt_agama.text.toString()) -> {
                    Toast.makeText(this@SplashScreenActivity,"Masukkan Agama", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                TextUtils.isEmpty(edt_asal.text.toString()) -> {
                    Toast.makeText(this@SplashScreenActivity,"Masukkan Asal Tempat", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                TextUtils.isEmpty(edt_status.text.toString()) -> {
                    Toast.makeText(this@SplashScreenActivity,"Masukkan Status", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                TextUtils.isEmpty(edt_jenis_kelamin.text.toString()) -> {
                    Toast.makeText(this@SplashScreenActivity,"Masukkan Jenis Kelamin", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                TextUtils.isEmpty(edt_tanggal_lahir.text.toString()) -> {
                    Toast.makeText(this@SplashScreenActivity,"Masukkan Tanggal Lahir", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                radiogroup.checkedRadioButtonId == -1 -> {
                    Toast.makeText(this@SplashScreenActivity,"Masukkan Jenis Mitra", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                else -> {
                    val model= DriverInfoModel()
                    model.firstName=edt_first_name.text.toString()
                    model.lastName=edt_last_name.text.toString()
                    model.phoneNumber=edt_phone_number.text.toString()
                    model.rating=5.0

                    val user: FirebaseUser? = auth.currentUser
                    val userId:String = user!!.uid
                    model.userId = userId

                    //additional
                    model.idNumber=edt_id_number.text.toString()
                    model.motorType=edt_motor_type.text.toString()
                    model.driveLicense=edt_drive_license.text.toString()
                    model.vehicleLicenseNumber=edt_vehicle_license.text.toString()

                    model.agama=edt_agama.text.toString()
                    model.address=edt_address.text.toString()
                    model.status=edt_status.text.toString()
                    model.jenisKelamin=edt_jenis_kelamin.text.toString()
                    model.tanggalLahir=edt_tanggal_lahir.text.toString()
                    model.asal=edt_asal.text.toString()

                    if (motor.isChecked){
                        model.typeMitra="Motor"
                    }
                    else if (mobil.isChecked){
                        model.typeMitra="Mobil"
                    }


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


//                            goToVerifyActivity()
                            goToHomeActivity(model)

                            progress_bar.visibility= View.GONE
                        }
                }
            }
        }
    }

    private fun goToHomeActivity(model: DriverInfoModel?) {
        Comon.currentUser=model
        startActivity(Intent(this, DriverHomeActivity::class.java))
        finish()
    }
    private fun goToVerifyActivity() {
        startActivity(Intent(this, VerifyDriverActivity::class.java))
        finish()
    }



}