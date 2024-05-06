package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.Observation
import com.example.myapplication.databinding.ActivityObservationsBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray

class ObservationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityObservationsBinding
    private lateinit var adapter: ObservationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityObservationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.addObservationsButton.setOnClickListener {
            val intent = Intent(this, AddObservationActivity::class.java)
            startActivity(intent)
        }
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
        // Set up the RecyclerView
        binding.observationsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize the adapter with an empty list
        adapter = ObservationsAdapter(emptyList())
        binding.observationsRecyclerView.adapter = adapter

        // Fetch observations from Firestore
        fetchObservations()
    }
    private fun fetchObservations() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return // Use the current user's ID
        val db = FirebaseFirestore.getInstance()
        db.collection("observations").document(userId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val observationsArray = documentSnapshot.get("observations") as? List<Map<String, Any>> // Safe cast
                val observationsList = observationsArray?.mapNotNull { observationMap ->
                    // Safe casts for each expected property.
                    Observation(
                        birdName = observationMap["birdName"] as? String ?: "Unknown",
                        date = observationMap["date"] as? String ?: "Unknown Date",
                        description = observationMap["description"] as? String ?: "No Description",
                        imageUri = observationMap["imageUri"] as? String ?: "",
                        latitude = (observationMap["latitude"] as? Double) ?: 0.0,
                        longitude = (observationMap["longitude"] as? Double) ?: 0.0
                    )
                } ?: emptyList() // If the cast fails, provide an empty list.

                // Update the adapter with the new list
                adapter.updateObservations(observationsList)
                adapter.notifyDataSetChanged() // Notify the adapter to refresh the data
            }
            .addOnFailureListener { e ->
            showToast("Error display observations: ${e.message}")
        }
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}

