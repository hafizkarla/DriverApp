package com.kinikumuda.multikurir_driverapp.ui.home

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.*
import android.widget.Toast.LENGTH_SHORT
import android.widget.Toast.makeText
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.kinikumuda.multikurir_driverapp.Comon
import com.kinikumuda.multikurir_driverapp.DriverHomeActivity
import com.kinikumuda.multikurir_driverapp.Model.EventBus.DriverRequestReceived
import com.kinikumuda.multikurir_driverapp.Model.RiderModel
import com.kinikumuda.multikurir_driverapp.Model.TripPlanModel
import com.kinikumuda.multikurir_driverapp.R
import com.kinikumuda.multikurir_driverapp.Remote.IGoogleAPI
import com.kinikumuda.multikurir_driverapp.Remote.RetrofitClient
import com.kinikumuda.multikurir_driverapp.Utils.UserUtils
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import de.hdodenhof.circleimageview.CircleImageView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.io.IOException
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

class HomeFragment : Fragment(), OnMapReadyCallback {

    //media player
    private var mediaPlayer: MediaPlayer? = null
    //views
    private lateinit var btn_decline:Button
    private lateinit var layout_accept: CardView
    private lateinit var circularProgressBar:CircularProgressBar
    private lateinit var txt_estimate_time:TextView
    private lateinit var type_order:TextView
    private lateinit var txt_estimate_distance:TextView
    private lateinit var root_layout:FrameLayout

    private lateinit var btn_topup:Button


    private lateinit var txt_rating:TextView
    private lateinit var img_round:ImageView
    private lateinit var layout_start_uber:CardView
    private lateinit var layout_info_bojek:CardView
    private lateinit var txt_rider_name:TextView
    private lateinit var txt_rider_number:TextView
    private lateinit var txt_start_uber_estimate_distance:TextView
    private lateinit var txt_start_uber_estimate_time:TextView
    private lateinit var txt_start_uber_estimate_price:TextView
    private lateinit var txt_estimate_price:TextView
    private lateinit var btn_call_driver:CircleImageView
    private lateinit var btn_accept:Button
    private lateinit var btn_finish:CircleImageView
    private lateinit var txt_name:TextView
    private lateinit var txt_phone:TextView
    private lateinit var txt_motor_type:TextView
    private lateinit var txt_vehicle_number:TextView
    private lateinit var txt_rating_home:TextView

    private var isTripStart=false
    private var onlineSystemAlreadyRegister=false

    private var tripNumberId:String?=""
    private var phoneNumber:String?=""

    //routes
    private val compositeDisposable= CompositeDisposable()
    private lateinit var iGoogleAPI: IGoogleAPI
    private var blackPolyLine: Polyline?=null
    private var greyPolyline: Polyline?=null
    private var polylineOptions: PolylineOptions?=null
    private var blackPolylineOptions: PolylineOptions?=null
    private var polylineList:ArrayList<LatLng?>?=null

    private lateinit var mMap: GoogleMap
    private lateinit var homeViewModel: HomeViewModel

    private lateinit var mapFragment:SupportMapFragment

    //Location
    private var locationRequest:LocationRequest?=null
    private var locationCallback:LocationCallback?=null
    private var fusedLocationProviderClient:FusedLocationProviderClient?=null

    //online system
    private lateinit var onlineRef:DatabaseReference
    private var currentUserRef:DatabaseReference?=null
    private lateinit var driverLocationRef:DatabaseReference
    private lateinit var geoFire: GeoFire

    //decline
    private var driverRequestReceived: DriverRequestReceived?=null
    private var countDownEvent:Disposable?=null

    //format rupiah
    val localeId = Locale("in", "ID")
    val formatRupiah = NumberFormat.getCurrencyInstance(localeId)

    //accept flag
    var accept:Boolean=false
    var valueCheck:Boolean=true

    private val onlineValueEventListener=object:ValueEventListener{
        override fun onDataChange(p0: DataSnapshot) {
            if(p0.exists() && currentUserRef != null)
                currentUserRef!!.onDisconnect().removeValue()

        }

        override fun onCancelled(p0: DatabaseError) {
            Snackbar.make(mapFragment.requireView(), p0.message, Snackbar.LENGTH_LONG).show()
        }

    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)



    }
    override fun onDestroy() {
        fusedLocationProviderClient!!.removeLocationUpdates(locationCallback)
        geoFire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
        onlineRef.removeEventListener(onlineValueEventListener)

        compositeDisposable.clear()

        onlineSystemAlreadyRegister=false

        if (EventBus.getDefault().hasSubscriberForEvent(DriverHomeActivity::class.java))
            EventBus.getDefault().removeStickyEvent(DriverHomeActivity::class.java)
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun onResume() {

        super.onResume()
        registerOnlineSystem()
    }

    private fun registerOnlineSystem() {
        if (!onlineSystemAlreadyRegister) {
            onlineRef.addValueEventListener(onlineValueEventListener)
            onlineSystemAlreadyRegister=true
        }
    }




    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        mediaPlayer=MediaPlayer.create(context, R.raw.sound1)
        mediaPlayer?.setOnPreparedListener {
            println("READY TO GO")
        }

        val setting = context?.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val checkBox = setting?.getBoolean("aktif", true)
        if (checkBox==true)
            root.mode_true.isChecked = true
        else
            root.mode_true.isChecked = false



        root.mode_true.setOnClickListener {
            if (root.mode_true.isChecked==true) {
                valueCheck = true
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Snackbar.make(
                        mapFragment.requireView(),
                        getString(R.string.permission_require),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                fusedLocationProviderClient!!.lastLocation
                    .addOnFailureListener { e->
                        Snackbar.make(mapFragment.requireView(), e.message!!, Snackbar.LENGTH_SHORT).show()
                    }
                    .addOnSuccessListener { location->

                        makeDriverOnline(location)

                    }
            }
            else{
                valueCheck = false
                currentUserRef!!.removeValue()
            }
            val mSettings: SharedPreferences =requireActivity().getSharedPreferences(
                "Settings",
                Context.MODE_PRIVATE
            )
            val editor = mSettings.edit()
            editor.putBoolean("aktif",valueCheck)
            editor.apply()
        }



        initViews(root)
        init()

        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return root
    }




    private fun initViews(root: View?) {
        btn_accept=root!!.findViewById(R.id.btn_accept) as Button
        btn_topup=root.findViewById(R.id.btn_topup) as Button
        btn_decline=root.findViewById(R.id.btn_decline) as Button
        layout_accept= root.findViewById(R.id.layout_accept) as CardView
        circularProgressBar= root.findViewById(R.id.circularProgressBar) as CircularProgressBar
        txt_estimate_time= root.findViewById(R.id.txt_estimate_time) as TextView
        type_order=root.findViewById(R.id.type_order) as TextView
        txt_estimate_distance= root.findViewById(R.id.txt_estimate_distance) as TextView
        txt_estimate_price=root.findViewById(R.id.txt_estimate_price) as TextView
        root_layout= root.findViewById(R.id.root_layout) as FrameLayout

        txt_rating = root.findViewById(R.id.txt_rating) as TextView
        img_round= root.findViewById(R.id.img_round) as ImageView
        layout_start_uber= root.findViewById(R.id.layout_start_uber) as CardView
        layout_info_bojek=root.findViewById(R.id.layout_info_bojek) as CardView
        txt_rider_name = root.findViewById(R.id.txt_rider_name) as TextView
        txt_rider_number=root.findViewById(R.id.txt_rider_number) as TextView
        txt_start_uber_estimate_distance= root.findViewById(R.id.txt_start_uber_estimate_distance) as TextView
        txt_start_uber_estimate_price=root.findViewById(R.id.txt_start_uber_estimate_price) as TextView
        txt_start_uber_estimate_time= root.findViewById(R.id.txt_start_uber_estimate_time) as TextView
        btn_call_driver= root.findViewById(R.id.btn_call_driver) as CircleImageView
        btn_finish=root.findViewById(R.id.btn_finish) as CircleImageView

        txt_name=root.findViewById(R.id.txt_name) as TextView
        txt_phone=root.findViewById(R.id.txt_phone) as TextView
        txt_motor_type=root.findViewById(R.id.txt_motor_type) as TextView
        txt_vehicle_number=(root.findViewById(R.id.txt_vehicle_number) as? TextView)!!
        txt_rating_home= (root.findViewById(R.id.txt_rating_home) as? TextView)!!

        txt_name.text =Comon.currentUser!!.firstName+" "+ Comon.currentUser!!.lastName
        txt_phone.text=Comon.currentUser!!.phoneNumber
        txt_motor_type.text=Comon.currentUser!!.motorType
        txt_vehicle_number.text=Comon.currentUser!!.vehicleLicenseNumber
        txt_rating_home.text= Comon.currentUser!!.rating.toString()

        //event
        btn_decline.setOnClickListener {
            if (driverRequestReceived!=null)
            {
                if (countDownEvent!=null)
                {

                    countDownEvent!!.dispose()
                    layout_info_bojek.visibility=View.VISIBLE
                    layout_accept.visibility=View.GONE
                    mediaPlayer?.stop()
                    mMap.clear()
                    circularProgressBar.progress=0f
                    UserUtils.sendDeclineRequest(
                        root_layout,
                        activity!!,
                        driverRequestReceived!!.key!!
                    )
                    driverRequestReceived=null

                }
            }
        }



        btn_accept.setOnClickListener {
            accept=true
            mediaPlayer?.stop()
            circularProgressBar.progress=100f
            Toast.makeText(
                context, "Order berhasil diterima. Mohon menunggu sesaat...",
                Toast.LENGTH_LONG
            ).show()

        }

        btn_finish.setOnClickListener {
            val update_trip=HashMap<String, Any>()
            update_trip.put("done", true)
            FirebaseDatabase.getInstance()
                .getReference(Comon.TRIP)
                .child(tripNumberId!!)
                .updateChildren(update_trip)
                .addOnFailureListener{ e->Snackbar.make(
                    requireView(),
                    e.message!!,
                    Snackbar.LENGTH_LONG
                ).show()}
                .addOnSuccessListener {
                    fusedLocationProviderClient!!.lastLocation
                        .addOnFailureListener { e->
                            Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_LONG).show()
                        }.addOnSuccessListener { location->
                            UserUtils.sendDone(
                                mapFragment.requireView(),
                                requireContext(),
                                driverRequestReceived!!.key,
                                tripNumberId!!
                            )
                            //reset view

                            tripNumberId=""
                            isTripStart=false
                            layout_accept.visibility=View.GONE
                            circularProgressBar.progress=0.toFloat()
                            layout_start_uber.visibility=View.GONE

                            btn_finish.isEnabled=false
                            btn_finish.visibility=View.GONE
                            layout_info_bojek.visibility =View.VISIBLE

                            driverRequestReceived=null
                            makeDriverOnline(location)


                            mMap.clear()

                        }
                }
        }

        btn_call_driver.setOnClickListener {
            checkPermission()
        }





    }

    private fun init() {


        iGoogleAPI= RetrofitClient.instance!!.create(IGoogleAPI::class.java)

            onlineRef = FirebaseDatabase.getInstance().reference.child(".info/connected")




        //registerOnlineSystem()
        //if permission is not allow,dont init it,let user allow it first
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Snackbar.make(root_layout, getString(R.string.permission_require), Snackbar.LENGTH_LONG).show()
            return
        }
        buildLocationRequest()
        buildLocationCallback()
        updateLocation()

    }



    private fun updateLocation() {
        if (fusedLocationProviderClient==null)
        {
            fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(
                requireContext()
            )
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Snackbar.make(
                    root_layout,
                    getString(R.string.permission_require),
                    Snackbar.LENGTH_LONG
                ).show()
                return
            }
            fusedLocationProviderClient!!.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper()
            )
        }
    }

    private fun buildLocationCallback() {
        if (locationCallback==null)
        {
            locationCallback=object: LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    super.onLocationResult(locationResult)

                    val newPos=LatLng(
                        locationResult!!.lastLocation.latitude,
                        locationResult.lastLocation.longitude
                    )
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos, 18f))

                    if (!isTripStart && valueCheck==true) {
                        makeDriverOnline(locationResult.lastLocation!!)
                    }
                    else
                    {
                        if (!TextUtils.isEmpty(tripNumberId))
                        {
                            //update location
                            val update_data=HashMap<String, Any>()
                            update_data["currentLat"]=locationResult.lastLocation.latitude
                            update_data["currentLng"]=locationResult.lastLocation.longitude

                            FirebaseDatabase.getInstance().getReference(Comon.TRIP)
                                .child(tripNumberId!!)
                                .updateChildren(update_data)
                                .addOnFailureListener { e->
                                    Snackbar.make(
                                        mapFragment.requireView(),
                                        e.message!!,
                                        Snackbar.LENGTH_LONG
                                    ).show()
                                }.addOnSuccessListener {  }
                        }
                    }


                }
            }
        }
    }

    private fun makeDriverOnline(location: Location) {
        val geoCoder = Geocoder(requireContext(), Locale.getDefault())
        val addressList: List<Address>?
        try {
            addressList = geoCoder.getFromLocation(
                location.latitude,
                location.longitude,
                1
            )
            val cityName = addressList[0].locality

            driverLocationRef = FirebaseDatabase.getInstance()
                .getReference(Comon.DRIVER_LOCATION_REFERENCE)
                .child(cityName)
            currentUserRef = driverLocationRef.child(
                FirebaseAuth.getInstance().currentUser!!.uid
            )
            geoFire = GeoFire(driverLocationRef)
            //update location
            geoFire.setLocation(
                FirebaseAuth.getInstance().currentUser!!.uid,
                GeoLocation(
                    location.latitude,
                    location.longitude
                ),

                ) { key: String?, error: DatabaseError? ->
                if (error != null)
                    Snackbar.make(
                        mapFragment.requireView(),
                        error.message,
                        Snackbar.LENGTH_LONG
                    ).show()


            }

            registerOnlineSystem()


        } catch (e: IOException) {
            Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_SHORT).show()
        }

    }

    private fun buildLocationRequest() {
        if (locationRequest==null)
        {
            locationRequest= LocationRequest()
            locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest!!.fastestInterval = 15000
            locationRequest!!.interval=10000
            locationRequest!!.smallestDisplacement = 50f
        }
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap!!

        //Request permission
        Dexter.withContext(requireContext())
            .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    //enable button
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        Snackbar.make(
                            root_layout,
                            getString(R.string.permission_require),
                            Snackbar.LENGTH_LONG
                        ).show()
                        return
                    }
                    mMap.isMyLocationEnabled = true
                    mMap.uiSettings.isMyLocationButtonEnabled = true
                    mMap.setOnMyLocationClickListener {

                        fusedLocationProviderClient!!.lastLocation
                            .addOnFailureListener { e ->
                                makeText(context!!, e.message, LENGTH_SHORT).show()
                            }.addOnSuccessListener { location ->
                                val userLatLng = LatLng(location.latitude, location.longitude)
                                mMap.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        userLatLng,
                                        18f
                                    )
                                )
                            }
                        true
                    }
                    //Layout
                    val locationButton = (mapFragment.requireView()
                        .findViewById<View>("1".toInt())
                        .parent!! as View).findViewById<View>("2".toInt())
                    val params = locationButton.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    params.bottomMargin = 50

                    //location
                    buildLocationRequest()
                    buildLocationCallback()
                    updateLocation()


                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    makeText(
                        context!!, "Permission " + p0!!.permissionName + " was denied",
                        LENGTH_SHORT
                    ).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                }


            }).check()

        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    R.raw.uber_maps_style
                )
            )
            if (!success)
                Log.e("EDMT_ERROR", "Style parsing error.")
        } catch (e: Resources.NotFoundException)
        {
            Log.e("EDMT_ERROR", e.message.toString())
        }
        

    }

    @SuppressLint("SetTextI18n")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onDriverRequestReceived(event: DriverRequestReceived)
    {
        driverRequestReceived=event
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Snackbar.make(
                requireView(),
                getString(R.string.permission_require),
                Snackbar.LENGTH_LONG
            ).show()
            return
        }
        fusedLocationProviderClient!!.lastLocation
            .addOnFailureListener{ e->
                Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_LONG).show()
            }
            .addOnSuccessListener { location->
                compositeDisposable.add(iGoogleAPI.getDirection(
                    "driving",
                    "less_driving",
                    StringBuilder()
                        .append(location.latitude)
                        .append(",")
                        .append(location.longitude)
                        .toString(),
                    event.destinationLocation,
                    getString(R.string.google_api_key)
                )
                !!.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { returnResult ->
                        Log.d("API_RETURN", returnResult)
                        try {

                            val jsonObject = JSONObject(returnResult)
                            val jsonArray = jsonObject.getJSONArray("routes")
                            for (i in 0 until jsonArray.length()) {
                                val route = jsonArray.getJSONObject(i)
                                val poly = route.getJSONObject("overview_polyline")
                                val polyline = poly.getString("points")
                                polylineList = Comon.decodePoly(polyline)

                            }
                            polylineOptions = PolylineOptions()
                            polylineOptions!!.color(R.color.colorPrimary)
                            polylineOptions!!.width(12f)
                            polylineOptions!!.startCap(SquareCap())
                            polylineOptions!!.jointType(JointType.ROUND)
                            polylineOptions!!.addAll(polylineList)
                            greyPolyline = mMap.addPolyline(polylineOptions)

                            blackPolylineOptions = PolylineOptions()
                            blackPolylineOptions!!.color(R.color.colorPrimary)
                            blackPolylineOptions!!.width(5f)
                            blackPolylineOptions!!.startCap(SquareCap())
                            blackPolylineOptions!!.jointType(JointType.ROUND)
                            blackPolylineOptions!!.addAll(polylineList)
                            blackPolyLine = mMap.addPolyline(blackPolylineOptions)

                            //animator
                            val valueAnimator = ValueAnimator.ofInt(0, 100)
                            valueAnimator.duration = 1100
                            valueAnimator.repeatCount = ValueAnimator.INFINITE
                            valueAnimator.interpolator = LinearInterpolator()
                            valueAnimator.addUpdateListener { value ->
                                val points = greyPolyline!!.points
                                val percentValue = value.animatedValue.toString().toInt()
                                val size = points.size
                                val newpoints = (size * (percentValue / 100.0f)).toInt()
                                val p = points.subList(0, newpoints)
                                blackPolyLine!!.points = (p)

                            }

                            valueAnimator.start()

                            val origin = LatLng(location.latitude, location.longitude)
                            val destination = LatLng(
                                event.pickupLocation!!.split(",")[0].toDouble(),
                                event.pickupLocation!!.split(",")[1].toDouble()
                            )

                            val latLngBound = LatLngBounds.Builder().include(origin)
                                .include(destination)
                                .build()

                            //add car icon for origin
                            val objects = jsonArray.getJSONObject(0)
                            val legs = objects.getJSONArray("legs")
                            val legsObject = legs.getJSONObject(0)

                            val time = legsObject.getJSONObject("duration")
                            val duration = time.getString("text")


                            val distanceEstimate = legsObject.getJSONObject("distance")
                            val distance = distanceEstimate.getString("text")

                            txt_estimate_time.text = duration
                            txt_estimate_distance.text = distance
                            type_order.text = event.typeOrder


                            if (event.typeOrder.equals("Ojek Mobil") && distance.substring(0, 3)
                                    .toFloat() >= 5
                            ) {
                                txt_estimate_price.text = formatRupiah.format(
                                    (35000 + ((distance.substring(
                                        0,
                                        3
                                    ).toFloat() - 5) * 5000).toInt()).toDouble()
                                )
                            } else if (event.typeOrder.equals("Ojek Mobil") && distance.substring(
                                    0,
                                    3
                                ).toFloat() < 5
                            ) {
                                txt_estimate_price.text = "Rp. 35.000"
                            } else if (event.typeOrder.equals("Ojek Motor") && distance.substring(
                                    0,
                                    3
                                ).toFloat() >= 3
                            ) {
                                txt_estimate_price.text = formatRupiah.format(
                                    (9000 + ((distance.substring(
                                        0,
                                        3
                                    ).toFloat() - 3) * 1800).toInt()).toDouble()
                                )
                            } else if (event.typeOrder.equals("Ojek Motor") && distance.substring(
                                    0,
                                    3
                                ).toFloat() < 3
                            ) {
                                txt_estimate_price.text = "Rp. 9.000"
                            } else if (event.typeOrder.equals("Ojek Kurir") && distance.substring(
                                    0,
                                    3
                                ).toFloat() >= 3
                            ) {
                                txt_estimate_price.text = formatRupiah.format(
                                    (9000 + ((distance.substring(
                                        0,
                                        3
                                    ).toFloat() - 3) * 1800).toInt()).toDouble()
                                )
                            } else if (event.typeOrder.equals("Ojek Kurir") && distance.substring(
                                    0,
                                    3
                                ).toFloat() < 3
                            ) {
                                txt_estimate_price.text = "Rp. 9.000"
                            } else {
                                makeText(
                                    requireContext(),
                                    "Error saat memuat harga",
                                    Toast.LENGTH_LONG
                                ).show()
                            }



                            mMap.addMarker(
                                MarkerOptions().position(destination).icon(
                                    BitmapDescriptorFactory.defaultMarker()
                                )
                                    .title("Pickup Location")
                            )

                            mMap.moveCamera(
                                CameraUpdateFactory.newLatLngBounds(
                                    latLngBound,
                                    160
                                )
                            )
                            mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition!!.zoom - 1))

                            //display layout
                            layout_accept.visibility = View.VISIBLE
                            layout_info_bojek.visibility = View.GONE

                            mediaPlayer?.start()

                            //countdown
                            countDownEvent = Observable.interval(100, TimeUnit.MILLISECONDS)
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnNext { x ->
                                    circularProgressBar.progress += 1f
                                }
                                .takeUntil { aLong -> aLong == "100".toLong() } //10sec
                                .doOnComplete {
                                    createTripPlan(event, duration, distance)
                                }.subscribe()

                        } catch (e: java.lang.Exception) {
                            makeText(requireActivity(), e.message!!, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
    }



    private fun createTripPlan(event: DriverRequestReceived, duration: String, distance: String) {
        setLayoutProcess(true)
        //sync server time with device
        FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val timeOffset = snapshot.getValue(Long::class.java)


                    //load rider information
                    FirebaseDatabase.getInstance()
                        .getReference(Comon.RIDER_INFO)
                        .child(event.key!!)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            @SuppressLint("SetTextI18n")
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    var riderModel = snapshot.getValue(RiderModel::class.java)


                                    //get location
                                    if (ActivityCompat.checkSelfPermission(
                                            requireContext(),
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                            requireContext(),
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        Snackbar.make(
                                            mapFragment.requireView(),
                                            requireContext().getString(R.string.permission_require),
                                            Snackbar.LENGTH_LONG
                                        ).show()
                                        return
                                    }

                                    fusedLocationProviderClient!!.lastLocation
                                        .addOnFailureListener { e ->
                                            Snackbar.make(
                                                mapFragment.requireView(),
                                                e.message!!,
                                                Snackbar.LENGTH_LONG
                                            ).show()
                                        }
                                        .addOnSuccessListener { location ->
                                            //create trip planner
                                            val tripPlanModel = TripPlanModel()
                                            tripPlanModel.driver =
                                                FirebaseAuth.getInstance().currentUser!!.uid
                                            tripPlanModel.rider = event.key
                                            tripPlanModel.driverInfoModel = Comon.currentUser
                                            tripPlanModel.riderModel = riderModel
                                            tripPlanModel.origin = event.pickupLocation
                                            tripPlanModel.originString = event.pickupLocationString
                                            tripPlanModel.destination = event.destinationLocation
                                            tripPlanModel.destinationString =
                                                event.destinationLocationString
                                            tripPlanModel.distancePickup = distance
                                            tripPlanModel.durationPickup = duration
                                            tripPlanModel.currentLat = location.latitude
                                            tripPlanModel.currentLng = location.longitude
                                            tripPlanModel.typeOrder = event.typeOrder

                                            if (event.typeOrder.equals("Ojek Mobil") && distance.substring(
                                                    0,
                                                    3
                                                ).toFloat() >= 5
                                            ) {
                                                tripPlanModel.price = formatRupiah.format(
                                                    (35000 + ((distance.substring(
                                                        0,
                                                        3
                                                    ).toFloat() - 5) * 5000).toInt()).toDouble()
                                                )
                                            } else if (event.typeOrder.equals("Ojek Mobil") && distance.substring(
                                                    0,
                                                    3
                                                ).toFloat() < 5
                                            ) {
                                                tripPlanModel.price = "Rp. 35.000"
                                            } else if (event.typeOrder.equals("Ojek Motor") && distance.substring(
                                                    0,
                                                    3
                                                ).toFloat() >= 3
                                            ) {
                                                tripPlanModel.price = formatRupiah.format(
                                                    (9000 + ((distance.substring(
                                                        0,
                                                        3
                                                    ).toFloat() - 3) * 1800).toInt()).toDouble()
                                                )
                                            } else if (event.typeOrder.equals("Ojek Motor") && distance.substring(
                                                    0,
                                                    3
                                                ).toFloat() < 3
                                            ) {
                                                tripPlanModel.price = "Rp. 9.000"
                                            } else if (event.typeOrder.equals("Ojek Kurir") && distance.substring(
                                                    0,
                                                    3
                                                ).toFloat() >= 3
                                            ) {
                                                tripPlanModel.price = formatRupiah.format(
                                                    (9000 + ((distance.substring(
                                                        0,
                                                        3
                                                    ).toFloat() - 3) * 1800).toInt()).toDouble()
                                                )
                                            } else if (event.typeOrder.equals("Ojek Kurir") && distance.substring(
                                                    0,
                                                    3
                                                ).toFloat() < 3
                                            ) {
                                                tripPlanModel.price = "Rp. 9.000"
                                            }





                                            tripNumberId =
                                                Comon.createUniqueTripIdNumber(timeOffset)


                                            //submit
                                            FirebaseDatabase.getInstance().getReference(Comon.TRIP)
                                                .child(tripNumberId!!)
                                                .setValue(tripPlanModel)
                                                .addOnFailureListener { e ->
                                                    Snackbar.make(
                                                        mapFragment.requireView(),
                                                        e.message!!,
                                                        Snackbar.LENGTH_LONG
                                                    ).show()
                                                }
                                                .addOnSuccessListener { aVoid ->
                                                    txt_rider_name.text =
                                                        riderModel!!.firstName + " " + riderModel!!.lastName
                                                    txt_rider_number.text = riderModel.phoneNumber
                                                    phoneNumber = riderModel.phoneNumber
                                                    txt_start_uber_estimate_distance.text = distance
                                                    txt_start_uber_estimate_time.text = duration

                                                    if (event.typeOrder.equals("Ojek Mobil") && distance.substring(
                                                            0,
                                                            3
                                                        ).toFloat() >= 5
                                                    ) {
                                                        txt_start_uber_estimate_price.text =
                                                            formatRupiah.format(
                                                                (35000 + ((distance.substring(
                                                                    0,
                                                                    3
                                                                )
                                                                    .toFloat() - 5) * 5000).toInt()).toDouble()
                                                            )
                                                    } else if (event.typeOrder.equals("Ojek Mobil") && distance.substring(
                                                            0,
                                                            3
                                                        ).toFloat() < 5
                                                    ) {
                                                        txt_start_uber_estimate_price.text =
                                                            "Rp. 35.000"
                                                    } else if (event.typeOrder.equals("Ojek Motor") && distance.substring(
                                                            0,
                                                            3
                                                        ).toFloat() >= 3
                                                    ) {
                                                        txt_start_uber_estimate_price.text =
                                                            formatRupiah.format(
                                                                (9000 + ((distance.substring(
                                                                    0,
                                                                    3
                                                                )
                                                                    .toFloat() - 3) * 1800).toInt()).toDouble()
                                                            )
                                                    } else if (event.typeOrder.equals("Ojek Motor") && distance.substring(
                                                            0,
                                                            3
                                                        ).toFloat() < 3
                                                    ) {
                                                        txt_start_uber_estimate_price.text =
                                                            "Rp. 9.000"
                                                    } else if (event.typeOrder.equals("Ojek Kurir") && distance.substring(
                                                            0,
                                                            3
                                                        ).toFloat() >= 3
                                                    ) {
                                                        txt_start_uber_estimate_price.text =
                                                            formatRupiah.format(
                                                                (9000 + ((distance.substring(
                                                                    0,
                                                                    3
                                                                )
                                                                    .toFloat() - 3) * 1800).toInt()).toDouble()
                                                            )
                                                    } else if (event.typeOrder.equals("Ojek Kurir") && distance.substring(
                                                            0,
                                                            3
                                                        ).toFloat() < 3
                                                    ) {
                                                        txt_start_uber_estimate_price.text =
                                                            "Rp. 9.000"
                                                    } else {
                                                        makeText(
                                                            requireContext(),
                                                            "Error saat memuat harga",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }


                                                }

                                            if (accept) {
                                                circularProgressBar.progress = 100f
                                                layout_accept.visibility = View.GONE
                                                setOfflineModeForDriver(
                                                    event,
                                                    duration,
                                                    distance
                                                )
                                            }
                                            if (!accept) {
                                                countDownEvent!!.dispose()
                                                layout_info_bojek.visibility = View.VISIBLE
                                                layout_accept.visibility = View.GONE
                                                mediaPlayer?.stop()
                                                mMap.clear()
                                                circularProgressBar.progress = 0f
                                                UserUtils.sendDeclineRequest(
                                                    root_layout,
                                                    activity!!,
                                                    driverRequestReceived!!.key!!
                                                )
                                                driverRequestReceived = null

                                            }


                                        }

                                } else
                                    Snackbar.make(
                                        mapFragment.requireView(), requireContext().getString(
                                            R.string.rider_not_found
                                        ) + " " + event.key!!, Snackbar.LENGTH_LONG
                                    ).show()
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Snackbar.make(
                                    mapFragment.requireView(),
                                    error.message,
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }

                        })

                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(mapFragment.requireView(), error.message, Snackbar.LENGTH_LONG)
                        .show()
                }

            })
    }
    fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CALL_PHONE
            )
            != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    Manifest.permission.CALL_PHONE
                )) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.CALL_PHONE),
                    42
                )
            }
        } else {
            // Permission has already been granted
            callPhone()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == 42) {
            // If request is cancelled, the result arrays are empty.
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // permission was granted, yay!
                callPhone()
            } else {
                // permission denied, boo! Disable the
                // functionality
            }
            return
        }
    }

    fun callPhone(){
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
        startActivity(intent)
    }



    private fun setOfflineModeForDriver(
        event: DriverRequestReceived,
        duration: String,
        distance: String
    ) {

        UserUtils.sendAcceptRequestToRider(
            mapFragment.view,
            requireContext(),
            event.key!!,
            tripNumberId
        )
        //go to offline
        if (currentUserRef != null) currentUserRef!!.removeValue()

        setLayoutProcess(false)
        layout_accept.visibility=View.GONE
        mediaPlayer?.stop()
        layout_start_uber.visibility=View.VISIBLE
        layout_info_bojek.visibility=View.GONE

        isTripStart=true
    }

    private fun setLayoutProcess(process: Boolean) {
        var color=-1
        if (process)
        {
            color = ContextCompat.getColor(requireContext(), R.color.dark_gray)
            circularProgressBar.indeterminateMode=true
            txt_rating.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_baseline_star_24_darkgray,
                0
            )
        }
        else{
            color = ContextCompat.getColor(requireContext(), android.R.color.white)
            circularProgressBar.indeterminateMode=false
            circularProgressBar.progress=0f
            txt_rating.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_baseline_star_24,
                0
            )
        }

        txt_estimate_time.setTextColor(color)
        txt_estimate_distance.setTextColor(color)
        type_order.setTextColor(color)
        txt_rating.setTextColor(color)
        ImageViewCompat.setImageTintList(img_round, ColorStateList.valueOf(color))

    }

    override fun onStop() {
        super.onStop()
        driverRequestReceived=null
        isTripStart=false
        currentUserRef!!.removeValue()

    }

}


