package com.example.sesion6

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.location.Location
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sesion6.databinding.ActivityLocationBinding
import com.example.sesion6.model.MyLocation
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.sql.Date

class LocationActivity : AppCompatActivity() {
    //constants
    val RADIUD_OF_EARTH_KM = 6371
    val latPUJ=4.627119
    val longPUJ =-74.04229
    //binding
    private lateinit var binding: ActivityLocationBinding
    private var currentLocation: Location? = null

    //permission
    val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback {
            if(it) {
                locationSettings()
            }else
                binding.latitude.text = "NO PERMISSION"

        }

    )

    val locationSettings = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
        ActivityResultCallback {
        if(it.resultCode== RESULT_OK){
            startLocationUpdates()
        }
        else{
            binding.altitude.text = "GPS OFF!"
        }

    })

    //location
    private lateinit var locationClient: FusedLocationProviderClient
    var locations = mutableListOf<JSONObject>()
    //request y callback
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback //tienen que ser del gms location
    //la dependencia en grade

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //obtener la localización. pero primero el permiso
        locationClient= LocationServices.getFusedLocationProviderClient(this)//es hija de activity entinces es base también
        locationRequest=createLocationRequest()
        locationCallback= createLocationCallback()

        //pedir el permiso
        locationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)



    }

    override fun onPause(){
        super.onPause()
        stopLocationUpdates()

    }


    private fun stopLocationUpdates(){
        locationClient.removeLocationUpdates(locationCallback)//si entra en pausa, cancelo suscripción de GPS
    }

    private fun createLocationCallback(): LocationCallback {//definir el objeto callback y retornarlo

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                val location= result.lastLocation
                if(location!=null){ //cada lectura del gps
                    if(currentLocation==null){
                        currentLocation=location
                    }else{
                        if(distance(currentLocation!!.latitude, currentLocation!!.longitude, location.latitude, location.longitude)>0.05){
                            currentLocation=location
                            persistLocation()
                        }
                    }
                    updateUI(location)
                }
            }
        }
        return callback
    }

    private fun startLocationUpdates(){

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==PackageManager.PERMISSION_GRANTED) {
            locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())//suscribirme a cambios, me falta la reacción a esos cambios (callback)
        }else{
            binding.latitude.text = "NO PERMISSION"
        }
    }

    private fun persistLocation(){
        val myLocation = MyLocation(Date(System.currentTimeMillis()), currentLocation!!.latitude, currentLocation!!.longitude)
        locations.add(myLocation.toJSON())
        val filename= "locations.json"
        val file = File(baseContext.getExternalFilesDir(null), filename)
        val output = BufferedWriter(FileWriter(file))
        output.write(locations.toString())
        output.close()
        Log.i("LOCATION", "File modified at path" + file)
    }

    private fun createLocationRequest(): LocationRequest{
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setWaitForAccurateLocation(true)//encadenar llamados al mismo método
            .setMinUpdateIntervalMillis(3000).build()
        return request

    }

    private fun updateUI(location:Location){

        binding.latitude.text = location.latitude.toString()
        binding.longitude.text = location.longitude.toString()
        binding.altitude.text = location.altitude.toString()
        Log.i("LOCATION", "NEW LOCATION: ${location.latitude}, ${location.longitude}, ${location.altitude}")
        val dist = distance(location.latitude, location.longitude, latPUJ, longPUJ)

        binding.distanceToPUJ.text = "${dist.toString()} km"


    }
    private fun locationSettings() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            .addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { locationSettingsRespose ->
            startLocationUpdates()

        }
        task.addOnFailureListener { excepcion ->
            if (excepcion is ResolvableApiException) {
                try {
                    var isr: IntentSenderRequest =IntentSenderRequest.Builder(excepcion.resolution).build()
                    locationSettings.launch(isr)

                } catch (sendEx: IntentSender.SendIntentException) {
                    binding.altitude.text = "Device with no GPS!"
                }
            }
        }
    }
    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat1 - lat2)
        val dLon = Math.toRadians(lon1 - lon2)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a),Math.sqrt(1-a))
        val result= RADIUD_OF_EARTH_KM * c
        return Math.round(result*1000.0)/1000.0
    }


}


