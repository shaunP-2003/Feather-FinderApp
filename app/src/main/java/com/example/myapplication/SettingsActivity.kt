package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class SettingsActivity : AppCompatActivity() {

    private var imperialSelected: Boolean = false
    private lateinit var rangeInput: EditText
    private lateinit var radioMetric: RadioButton
    private lateinit var radioImperial: RadioButton
    private lateinit var btnSaveSettings: Button

    // Firebase Firestore instance
    private val db = FirebaseFirestore.getInstance()

    // Firebase Auth instance
    private val auth = FirebaseAuth.getInstance()

    // ...

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize views
        rangeInput = findViewById<EditText>(R.id.rangeInput)
        radioMetric = findViewById<RadioButton>(R.id.radioMetric)
        radioImperial = findViewById<RadioButton>(R.id.radioImperial)

         btnSaveSettings = findViewById<Button>(R.id.btnSaveSettings)
        btnSaveSettings.setOnClickListener {
            saveAllSettings()
        }

        // ...

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
    private fun saveAllSettings() {
        // Save unit preference
        val unitPreference = if (imperialSelected) "imperial" else "metric"
        savePreferenceToFirestore("unit_preference", unitPreference)

        // Save range preference
        val rangePreference = rangeInput.text.toString().toIntOrNull() ?: 0
        savePreferenceToFirestore("range_preference", rangePreference)

        // Add other settings to save if needed
    }
    private fun savePreferenceToFirestore(prefKey: String, prefValue: Any) {
        // Get current user ID from FirebaseAuth
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            // Handle case where user is not logged in or there's no user
            return
        }

        // Create or update the preferences in Firestore
        db.collection("userSettings").document(userId)
            .set(mapOf(prefKey to prefValue), SetOptions.merge())
            .addOnSuccessListener {
                showToast("Preference saved successfully")
            }
            .addOnFailureListener { e ->
                showToast("Error saving: ${e.message}")
            }
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

