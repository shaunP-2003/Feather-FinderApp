package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityChallengesBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction

internal class ChallengesActivity : AppCompatActivity() {
    private var currentUser: FirebaseUser? = null
    private var currentUserId: String? = null
    private lateinit var db: FirebaseFirestore
    private lateinit var binding: ActivityChallengesBinding
    private lateinit var badgeIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        binding = ActivityChallengesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        badgeIcon = findViewById(R.id.badgeIcon)

        db = FirebaseFirestore.getInstance() // This call will never return null
        currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let {
            // Get the unique ID of the logged-in user
            currentUserId = it.uid
            // If the user is logged in, setup the activate challenge button
            val activateChallengeButton = findViewById<Button>(R.id.activateChallengeButton)
            activateChallengeButton.setOnClickListener { activateChallenge() }
        } ?: run {
            // Handle the case where the user is not logged in
            // Perhaps navigate back to the login screen or display a message
        }
        updateChallengeUI()
        checkChallengeCompletion();

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

    private fun activateChallenge() {
        val userId = currentUserId ?: return // Return early if userId is null

        val challengeData = hashMapOf(
            "isActive" to true,
            "observationCounter" to 0
        )

        db.collection("challenges").document(userId)
            .set(challengeData)
            .addOnSuccessListener {
                showToast("Challenge activated!")
            }
            .addOnFailureListener { e ->
                showToast("Failed to activate challenge: ${e.message}")
            }

    }
    private fun updateChallengeUI() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val challengeRef = FirebaseFirestore.getInstance().collection("challenges").document(userId)
        challengeRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val isActive = document.getBoolean("isActive") ?: false
                val observationCounter = document.getLong("observationCounter") ?: 0

                binding.textViewChallengeStatus.text = "Challenge Status: ${if (isActive) "Active" else "Not Active"}"
                binding.textViewObservationCounter.text = "Observations: $observationCounter"
            } else {
                showToast("Challenge data not found.")
            }
        }.addOnFailureListener { e ->
            showToast("Failed to load challenge data: ${e.message}")
        }
    }
    private fun checkChallengeCompletion() {
        // Assuming you retrieve the current count from Firestore
        val getuserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val challengeRef = FirebaseFirestore.getInstance().collection("challenges").document(getuserId)
        challengeRef.get().addOnSuccessListener { document ->
            val currentCount = document.getLong("observationCounter") ?: 0
            val CHALLENGE_GOAL = 2
            if (currentCount >= CHALLENGE_GOAL) {
                awardBadge()
            }
        }
    }

// Call this method in your onCreate after initializing the binding

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    private fun awardBadge() {
        badgeIcon.visibility = View.VISIBLE
        showToast("Challenge completed! Badge awarded!")
    }
    }









