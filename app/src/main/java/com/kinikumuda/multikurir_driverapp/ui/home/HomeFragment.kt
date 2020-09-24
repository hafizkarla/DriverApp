package com.kinikumuda.multikurir_driverapp.ui.home

import android.content.res.Resources
import android.graphics.Camera
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.kinikumuda.multikurir_driverapp.Comon
import com.kinikumuda.multikurir_driverapp.R
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.kinikumuda.multikurir_driverapp.ui.home.HomeViewModel
import java.io.IOException
import java.security.Permission
import java.util.*
import java.util.jar.Manifest

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var homeViewModel: HomeViewModel

    private lateinit var mapFragment:SupportMapFragment

    //Location
    private lateinit var locationRequest:LocationRequest
    private lateinit var locationCallback:LocationCallback
    private lateinit var fusedLocationProviderClient:FusedLocationProviderClient

    //online system
    private lateinit var onlineRef:DatabaseReference
    private var currentUserRef:DatabaseReference?=null
    private lateinit var driverLocationRef:DatabaseReference
    private lateinit var geoFire: GeoFire

    private val onlineValueEventListener=object:ValueEventListener{
        override fun onDataChange(p0: DataSnapshot) {
            if(p0.exists() && currentUserRef != null)
                currentUserRef!!.onDisconnect().removeValue()
        }

        override fun onCancelled(p0: DatabaseError) {
            Snackbar.make(mapFragment.requireView(),p0.message, Snackbar.LENGTH_LONG).show()
        }

    }

    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        geoFire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
        onlineRef.removeEventListener(onlineValueEventListener)
        super.onDestroy()
    }


    override fun onResume() {

        super.onResume()
        registerOnlineSystem()
    }

    private fun registerOnlineSystem() {
        onlineRef.addValueEventListener(onlineValueEventListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        init()

        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        return root
    }

    private fun init() {

        onlineRef=FirebaseDatabase.getInstance().getReference().child(".info/connected")



        registerOnlineSystem()

        locationRequest= LocationRequest()
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest.setFastestInterval(15000)
        locationRequest.interval=10000
        locationRequest.setSmallestDisplacement(50f)

        locationCallback=object: LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                val newPos=LatLng(locationResult!!.lastLocation.latitude,locationResult!!.lastLocation.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos,18f))

                val geoCoder = Geocoder(requireContext(), Locale.getDefault())
                val addressList:List<Address>?
                try{
                    addressList=geoCoder.getFromLocation(locationResult.lastLocation.latitude,locationResult.lastLocation.longitude,1)
                    val cityName=addressList[0].locality

                    driverLocationRef=FirebaseDatabase.getInstance().getReference(Comon.DRIVER_LOCATION_REFERENCE)
                        .child(cityName)
                    currentUserRef=driverLocationRef.child(
                        FirebaseAuth.getInstance().currentUser!!.uid
                    )
                    geoFire= GeoFire(driverLocationRef)
                    //update location
                    geoFire.setLocation(
                        FirebaseAuth.getInstance().currentUser!!.uid,
                        GeoLocation(locationResult.lastLocation.latitude,locationResult.lastLocation.longitude),

                        ){key:String?, error:DatabaseError? ->
                        if(error!=null)
                            Snackbar.make(mapFragment.requireView(),error.message,Snackbar.LENGTH_LONG).show()


                    }

                    registerOnlineSystem()


                }catch (e:IOException){
                    Snackbar.make(requireView(),e.message!!,Snackbar.LENGTH_SHORT).show()
                }


            }
        }

        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper())
    }



    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap!!

        //Request permission
        Dexter.withContext(requireContext()!!)
            .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object :PermissionListener{
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    //enable button
                    mMap.isMyLocationEnabled=true
                    mMap.uiSettings.isMyLocationButtonEnabled=true
                    mMap.setOnMyLocationClickListener {

                        fusedLocationProviderClient.lastLocation
                            .addOnFailureListener{ e->
                                Toast.makeText(context!!,e.message,Toast.LENGTH_SHORT).show()
                            }.addOnSuccessListener { location->
                                val userLatLng=LatLng(location.latitude,location.longitude)
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng,18f))
                            }
                        true
                    }
                    //Layout
                    val locationButton=(mapFragment.requireView()!!
                        .findViewById<View>("1".toInt())
                        .parent!! as View).findViewById<View>("2".toInt())
                    val params=locationButton.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP,0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE)
                    params.bottomMargin=50


                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(context!!,"Permission "+p0!!.permissionName+" was denied",Toast.LENGTH_SHORT).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    TODO("Not yet implemented")
                }


            }).check()

        try {
            val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context,R.raw.uber_maps_style))
            if (!success)
                Log.e("EDMT_ERROR", "Style parsing error.")
        } catch (e:Resources.NotFoundException)
        {
            Log.e("EDMT_ERROR", e.message.toString())
        }

        Snackbar.make(mapFragment.requireView(),"Kamu online!",Snackbar.LENGTH_SHORT).show()

    }
}