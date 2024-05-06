package com.example.myapplication


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.Marker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import android.location.Location
import com.google.android.gms.maps.model.PolylineOptions
import android.graphics.Color
import android.net.Uri
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.PolyUtil

class BirdHotspotMap : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var mMap: GoogleMap
    private lateinit var sensorManager: SensorManager
    private var lastKnownAzimuth: Float = 0f

    private var selectedMarker: Marker? = null
    private var selectedBirdInfo: String? = null
    private val  hotspotCircles = mutableListOf<Circle>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bird_hotspot_map)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        checkLocationPermission()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)


        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_hotspots -> {
                    // Handle Hotspots item click if needed
                    val intent = Intent(this, BirdHotspotMap::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_observations -> {
                    // Handle Observations item click
                    val intent = Intent(this, ObservationsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_challenges -> {
                    // Handle Challenges item click if needed
                    // Replace this with your code to navigate to ChallengesActivity
                    val intent = Intent(this, ChallengesActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_settings -> {
                    // Handle Settings item click
                    // Navigate to SettingsActivity
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false



            }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true

            mMap.setOnMyLocationChangeListener { location ->
                val userLatLng = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f))

                mMap.setOnMyLocationChangeListener(null)

                fetchObservationsAndDisplayOnMap()
                displayHotspotsOnMap()
            }
        }

        mMap.setOnMapClickListener { latLng ->
            hotspotCircles.forEach { circle ->
                if (isLocationWithinCircle(latLng, circle)) {
                    showTravelPrompt(circle.center)
                    return@setOnMapClickListener
                }
            }
        }

    }

    private fun fetchEBirdData(userLatLng: LatLng) {
        val ebirdApi = ApiClient.retrofit.create(MyApiService::class.java)
        val call = ebirdApi.getHotspots(userLatLng.latitude, userLatLng.longitude)

        call.enqueue(object : Callback<List<HotSpot>> {
            override fun onResponse(call: Call<List<HotSpot>>, response: Response<List<HotSpot>>) {
                if (response.isSuccessful) {
                    val hotspots = response.body()
                    hotspots?.let { hotspotsList ->
                        hotspotsList.forEach { hotspot ->
                            val hotspotLocation = LatLng(hotspot.lat, hotspot.lng)
                            val greenMarker = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)

                            mMap.addMarker(
                                MarkerOptions()
                                    .position(hotspotLocation)
                                    .title(hotspot.comName)
                                    .snippet("Bird Name: ${hotspot.comName}\nCount: ${hotspot.howMany}")
                                    .icon(greenMarker)
                            )

                            Log.d("HotSpot", "Common Name: ${hotspot.comName}, Lat: ${hotspot.lat}, Lng: ${hotspot.lng}")
                        }

                        Log.d("HotSpot", "API call was successful. Retrieved ${hotspotsList.size} hotspots.")
                    }
                } else {
                    Log.e("API Error", "API call was not successful. Response code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<HotSpot>>, t: Throwable) {
                Log.e("API Error", "API call failed with exception: ${t.message}")
            }
        })
    }


    private fun displayHotspotsOnMap() {

        HotspotDATA.birdHotspotAreas.forEach { hotspot ->
            val hotspotLatLng = LatLng(hotspot.lat, hotspot.lng)
            val circle = mMap.addCircle(
                CircleOptions()
                    .center(hotspotLatLng)
                    .radius(hotspot.radius.toDouble())
                    .strokeColor(Color.BLUE)
                    .fillColor(0x30FF0000)
            )
            hotspotCircles.add(circle)
        }
    }
    fun fetchObservationsAndDisplayOnMap() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Reference to the single document that contains the array of observations.
        val db = FirebaseFirestore.getInstance()
        val userObservationsRef = db.collection("observations").document(currentUserId)

        userObservationsRef.get().addOnSuccessListener { documentSnapshot ->
            val observations = documentSnapshot.get("observations") as List<Map<String, Any>>?
            observations?.forEach { observationMap ->
                // Assuming you have an Observation class that matches the fields in your Firestore.
                val observation = Observation(
                    birdName = observationMap["birdName"] as String,
                    date = observationMap["date"] as String,
                    description = observationMap["description"] as String,
                    imageUri = observationMap["imageUri"] as String,
                    latitude = observationMap["latitude"] as Double,
                    longitude = observationMap["longitude"] as Double
                )
                displayMarkerForObservation(observation)
            }
        }.addOnFailureListener { e ->
            showToast("Error fetching observations: ${e.message}")
        }
    }

    fun displayMarkerForObservation(observation: Observation) {
        val customMarkerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
        val observationLatLng = LatLng(observation.latitude, observation.longitude)
        val marker = mMap.addMarker(
            MarkerOptions()
                .position(observationLatLng)
                .title(observation.birdName)
                .snippet("Observation\nBirdSpotted: ${observation.birdName}\nDate: ${observation.date}")
                .icon(customMarkerIcon)
        )

        // Set a click listener for the marker
        mMap.setOnMarkerClickListener { clickedMarker ->
            if (clickedMarker.equals(marker)) {
                showObservationPrompt(observation)
                true
            } else {
                false
            }
        }
    }
    private fun navigateUsingGoogleMaps(destination: LatLng) {
        val gmmIntentUri = Uri.parse("google.navigation:q=${destination.latitude},${destination.longitude}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")

        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            Toast.makeText(this, "Google Maps app is not installed", Toast.LENGTH_SHORT).show()
        }
    }
    private fun isLocationWithinCircle(point: LatLng, circle: Circle): Boolean {
        val distance = FloatArray(2)
        Location.distanceBetween(
            point.latitude, point.longitude,
            circle.center.latitude, circle.center.longitude,
            distance
        )
        return distance[0] < circle.radius
    }
    private fun showObservationPrompt(observation: Observation) {
        AlertDialog.Builder(this)
            .setTitle("Observation Details")
            .setMessage("Bird Name: ${observation.birdName}\nDate: ${observation.date}\nDescription: ${observation.description}")
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showTravelPrompt(destination: LatLng) {
        AlertDialog.Builder(this)
            .setTitle("Navigate to Hotspot")
            .setMessage("Do you want to navigate to this hotspot?")
            .setPositiveButton("Yes") { _, _ ->
                navigateUsingGoogleMaps(destination)
            }
            .setNegativeButton("No", null)
            .show()
    }

    // Inside your `calculateAndShowRoute()` function
    private fun calculateAndShowRoute() {
        val selectedMarkerPosition = selectedMarker?.position
        val userLatLng = mMap.myLocation?.let { LatLng(it.latitude, it.longitude) }

        if (selectedMarkerPosition != null && userLatLng != null) {
            Log.d("Debug", "Calculating and showing route...") // Add this log message

            // Make a request to the Directions API to get detailed directions
            val origin = "${userLatLng.latitude},${userLatLng.longitude}"
            val destination = "${selectedMarkerPosition.latitude},${selectedMarkerPosition.longitude}"
            val apiKey = "AIzaSyDdndLLWIh0rL7PxW80oRwbHRt20Mc3MKM" // Replace with your API key
            val directionsService = DirectionsApiClient.directionsService
            val call = directionsService.getDirections(origin, destination, apiKey)

            call.enqueue(object : Callback<DirectionsResponse> {
                override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                    if (response.isSuccessful) {
                        val directionsResponse = response.body()
                        directionsResponse?.routes?.firstOrNull()?.legs?.firstOrNull()?.steps?.let { steps ->
                            val polylinePoints = mutableListOf<LatLng>()
                            steps.forEach { step ->
                                val points = step.polyline.points
                                val decodedPoints = PolyUtil.decode(points)
                                polylinePoints.addAll(decodedPoints)
                            }

                            // Draw the detailed route polyline on the map
                            val polylineOptions = PolylineOptions()
                                .addAll(polylinePoints)
                                .width(5f) // Line width
                                .color(Color.BLUE) // Line color

                            mMap.addPolyline(polylineOptions)
                        }
                    } else {
                        Log.e("Directions API Error", "Directions API request failed with response code: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    Log.e("Directions API Error", "Directions API request failed with exception: ${t.message}")
                }
            })
        } else {
            // Handle the case where marker position or user's location is not available
            Toast.makeText(this, "Location data not available", Toast.LENGTH_SHORT).show()
        }
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    // Function to draw a Polyline route on the map
    private fun drawRouteOnMap(startLatLng: LatLng, endLatLng: LatLng) {
        val polylineOptions = PolylineOptions()
            .add(startLatLng)
            .add(endLatLng)
            .width(5f) // Line width
            .color(Color.BLUE) // Line color

        mMap.addPolyline(polylineOptions)
    }

    private fun getDistance(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results)
        return results[0]
    }

    private fun getBearing(start: LatLng, end: LatLng): Float {
        val startLat = Math.toRadians(start.latitude)
        val startLng = Math.toRadians(start.longitude)
        val endLat = Math.toRadians(end.latitude)
        val endLng = Math.toRadians(end.longitude)

        val deltaLng = endLng - startLng

        val y = Math.sin(deltaLng) * Math.cos(endLat)
        val x = Math.cos(startLat) * Math.sin(endLat) - Math.sin(startLat) * Math.cos(endLat) * Math.cos(deltaLng)

        val bearing = Math.toDegrees(Math.atan2(y, x))
        return (bearing.toFloat() + 360f) % 360f
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                        mMap.isMyLocationEnabled = true
                    }
                } else {
                }
            }
        }
    }
}
