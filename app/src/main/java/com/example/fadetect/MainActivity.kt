package com.example.fadetect

import DatabaseHelper
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {

    private lateinit var startStopButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var signalStrengthTextView: TextView
    private var isGatheringData = false
    private var strength = 0
    private var latitude = 0.0
    private var longitude = 0.0

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var dbHelper: DatabaseHelper

    private val signalStrengthListener = object : PhoneStateListener() {
        @SuppressLint("SetTextI18n")
        @RequiresApi(Build.VERSION_CODES.Q)
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
            // Handle signal strength changes here
            // You can access the signal strength information using signalStrength.getGsmSignalStrength() or other methods
            strength = signalStrength.cellSignalStrengths[0].dbm // Example: GSM signal strength
            signalStrengthTextView.text = "Signal Strength: $strength"
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        applicationContext.getExternalFilesDir("")
        startStopButton = findViewById(R.id.startStopButton)
        statusTextView = findViewById(R.id.statusTextView)
        locationTextView = findViewById(R.id.locationTextView)
        signalStrengthTextView = findViewById(R.id.signalStrengthTextView)

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationRequest()
        locationCallback = object : LocationCallback() {
             @SuppressLint("SetTextI18n")
             override fun onLocationResult(locationResult: LocationResult) {
                locationResult?.let {
                    for (location in locationResult.locations) {
                        latitude    =  String.format("%.4f", location.latitude).toDouble()
                        longitude   = String.format("%.4f", location.longitude).toDouble()
                        locationTextView.text = "Location: $latitude, $longitude"

                        addToDb()
                    }
                }
            }
        }

        startStopButton.setOnClickListener {
            if (isGatheringData) {
                stopGatheringData()
            } else {
                checkPermissionsAndStartGatheringData()
            }
        }
    }

    private fun addToDb() {
        val rsrp = strength
        val lat = latitude
        val lon = longitude

        val std = DataModel(rsrp = rsrp, latitude = lat, longitude = lon)
        val status = dbHelper.insertData(std)
        //Check insert success
        if (status > -1) {
            println("Data added successfully")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermissionsAndStartGatheringData() {
        // Check permissions
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                PERMISSION_REQUEST_CODE
            )
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ),
                PERMISSION_REQUEST_CODE
            )
        } else {
            startGatheringData()
        }
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startGatheringData() {
        dbHelper = DatabaseHelper(this)

        // Start gathering signal power data
        telephonyManager.listen(signalStrengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)

        isGatheringData = true
        startStopButton.text = "Stop"
        statusTextView.text = "Status: Gathering data..."
    }

    @SuppressLint("SetTextI18n")
    private fun stopGatheringData() {
        telephonyManager.listen(signalStrengthListener, PhoneStateListener.LISTEN_NONE)
        fusedLocationClient.removeLocationUpdates(locationCallback)

        isGatheringData = false
        startStopButton.text = "Start"
        statusTextView.text = "Status: Stopped"
        locationTextView.text = "Location: NULL"
        signalStrengthTextView.text = "Signal Strength: NULL"

        val success = dbHelper.exportDatabase(this)
        if (success) {
            Toast.makeText(this, "Database exported successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to export database, please remove it manually from Download folder", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 1000 // Update interval in milliseconds (1 second)
            fastestInterval = 1000 // Fastest update interval in milliseconds (1 second)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }
}