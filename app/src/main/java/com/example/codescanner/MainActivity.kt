package com.example.codescanner

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.budiyev.android.codescanner.*
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import java.text.DateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    lateinit var codeScanner: CodeScanner
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationSettingsClient: SettingsClient
    lateinit var locationCallback: LocationCallback
    lateinit var locationRequest: LocationRequest
    lateinit var locationSettingsRequest: LocationSettingsRequest
    var currentLocation: Location? =null
    var lastUpdatedTime: String?=null
    var requestingLocationUpdates: Boolean = false

    var s = ""
    var requestCheckSettings = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
        restoreValuesFromBundle(savedInstanceState)

        val scannerView = findViewById<CodeScannerView>(R.id.scanner)
        codeScanner = CodeScanner(this, scannerView)

//        createLocationRequest()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ), 1
                )

                return
            }

        }


//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
//                    android.Manifest.permission.ACCESS_FINE_LOCATION
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                requestPermissions(
//                    arrayOf(
//                        android.Manifest.permission.ACCESS_FINE_LOCATION,
//                    ), 101
//                )
//                return
//            }
//        }


        codeScanner.camera = CodeScanner.CAMERA_BACK
        codeScanner.formats = CodeScanner.ALL_FORMATS
        codeScanner.autoFocusMode = AutoFocusMode.SAFE
        codeScanner.scanMode = ScanMode.SINGLE
        codeScanner.isAutoFocusEnabled = true
        codeScanner.isFlashEnabled = false

        codeScanner.decodeCallback = DecodeCallback {

//            Toast.makeText(this, "After Get Location func", Toast.LENGTH_SHORT).show()

            Log.d("After Get Location func", "Get Location")

            //Code for handshake between FE and Lab using current location

            runOnUiThread {

                var qrCode = it.text
                Log.d("QRcode", qrCode)
                if (s.equals(qrCode)) {
                    Toast.makeText(this, "Handshake is successful", Toast.LENGTH_LONG).show()
                    val i = Intent(this, MainActivity2::class.java)
                    startActivity(i)
                } else
                    Toast.makeText(this, "Handshake is unsuccessful", Toast.LENGTH_LONG).show()


                // Code for OTP validation
//                val qrText = it.text
//                Toast.makeText(this," $qrText", Toast.LENGTH_LONG).show()
//                Log.d("Code",qrText)
//                var phNo = qrText.subSequence(qrText.lastIndexOf("\n")+1,qrText.length)
//                Log.d("PhoneNum",phNo.toString())
//                val i = Intent(this,VerifyOTPActivity::class.java)
//                i.putExtra("PhoneNo",phNo)
//                startActivity(i)


            }

        }

        codeScanner.errorCallback = ErrorCallback {
            runOnUiThread {
                Toast.makeText(this, " ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
        scannerView.setOnClickListener {
            codeScanner.startPreview()
        }


    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("is_requesting_updates", requestingLocationUpdates)
        outState.putParcelable("last_known_location", currentLocation)
        outState.putString("last_updated_on", lastUpdatedTime)
    }

    private fun restoreValuesFromBundle(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("is_requesting_updates"))
                requestingLocationUpdates = savedInstanceState.getBoolean("is_requesting_updates")
            if (savedInstanceState.containsKey("last_known_location")) {
                currentLocation = savedInstanceState.getParcelable("last_known_location")!!;
            }

            if (savedInstanceState.containsKey("last_updated_on")) {
                lastUpdatedTime = savedInstanceState.getString("last_updated_on").toString()
            }
        }

        updateLocation()
    }


    override fun onResume() {
        getLocationUpdates()
        Handler().postDelayed({
            //Do something after 100ms
            getLocation()
            Log.d("onResume","onResume")

        }, 3000)

        codeScanner.startPreview()
        super.onResume()
    }

    override fun onPause() {
        removeLocationUpdates()
        codeScanner.releaseResources()
        super.onPause()
    }


    fun getLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                    ), 1
                )
                return
            }
        }
        val task = fusedLocationProviderClient.lastLocation

        task.addOnSuccessListener() {
            currentLocation = it

            if (it != null) {
                s = "Latitude=" + it.latitude + "\n" + "Longitude=" + it.longitude
                Log.d("Location", s)
                Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
            } else
                Toast.makeText(this, "Location is null", Toast.LENGTH_SHORT).show()
        }
    }

    fun init() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient((this))
        locationSettingsClient = LocationServices.getSettingsClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                currentLocation = locationResult.lastLocation
                lastUpdatedTime = DateFormat.getTimeInstance().format(Date())
                updateLocation()
            }
        }
        requestingLocationUpdates = false
        locationRequest = LocationRequest.create()
            .setInterval(10000)
            .setFastestInterval(5000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        locationSettingsRequest = builder.build()
        val task: Task<LocationSettingsResponse> =
            locationSettingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // ...
            Log.d("TAG", "onSuccess: settingsCheck")
//            Handler().postDelayed({
//                //Do something after 100ms
//            }, 100)

//            getLocation()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        this@MainActivity,
                        2
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun updateLocation() {
        val s = "Latitude=" + currentLocation?.latitude + "Longitude=" + currentLocation?.longitude
        Log.d("Location:", s)
    }

    private fun getLocationUpdates() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                    ), 1
                )
                return
            }
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

    }

    private fun removeLocationUpdates() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                    ), 1
                )
                return
            }
        }
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }
}