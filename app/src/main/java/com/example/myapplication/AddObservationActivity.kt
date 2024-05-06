package com.example.myapplication

import android.R
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityAddObservationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class AddObservationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddObservationBinding
    private lateinit var selectedImageUri: Uri
    private lateinit var observationsSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddObservationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupHotspotSpinner()
        observationsSharedPreferences = getSharedPreferences("Observations", Context.MODE_PRIVATE)
        binding.selectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1000)


        }
        binding.saveButton.setOnClickListener {
            val selectedHotspotName = binding.hotspotSpinner.selectedItem.toString()
            val selectedHotspot = HotspotDATA.birdHotspotAreas.find { it.id == selectedHotspotName }

            val observation = Observation(
                birdName = binding.editTextBirdName.text.toString(),
                description = binding.editTextDescription.text.toString(),
                date = binding.editTextDate.text.toString(),
                imageUri = selectedImageUri.toString(),
                latitude = selectedHotspot?.lat ?: 0.0,
                longitude = selectedHotspot?.lng ?: 0.0
            )

            uploadImageAndSaveObservation(observation)

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000 && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data!!
            binding.birdImageView.setImageURI(selectedImageUri)
        }
    }
    private fun setupHotspotSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item, // Use android.R here
            HotspotDATA.birdHotspotAreas.map { it.id } // Assuming you are using 'name' property
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.hotspotSpinner.adapter = adapter
    }
    private fun uploadImageAndSaveObservation(observation: Observation) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return // Return early if user is not logged in
        val storageRef = FirebaseStorage.getInstance().reference.child("images/$currentUserId/${UUID.randomUUID()}")

        storageRef.putFile(selectedImageUri)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { downloadUri ->
                    val updatedObservation = observation.copy(imageUri = downloadUri.toString())
                    // Now save the observation with the updated imageUri to Firestore
                    saveObservationToFirestore(updatedObservation)
                }
            }
            .addOnFailureListener { e ->
                showToast("Failed to upload image: ${e.message}")
            }
    }

    private fun saveObservationToFirestore(observation: Observation) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return // Return early if user is not logged in

        val db = FirebaseFirestore.getInstance()
        val userObservationsRef = db.collection("observations").document(currentUserId)

        val observationMap = mapOf(
            "birdName" to observation.birdName,
            "description" to observation.description,
            "date" to observation.date,
            "imageUri" to observation.imageUri,
            "latitude" to observation.latitude,
            "longitude" to observation.longitude
        )

        // Check if the document exists, if not, create it with the new observation
        userObservationsRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                // The document already exists, update it with the new observation
                userObservationsRef.update("observations", FieldValue.arrayUnion(observationMap)).addOnSuccessListener {
                    showToast("Observation saved successfully")
                    // incrementObservationCount() // If you have this method, call it here
                }.addOnSuccessListener {
                    showToast("Observation saved successfully")
                     incrementObservationCount() // If you have this method, call it here
                }.addOnFailureListener { e ->
                    showToast("Failed to save observation: ${e.message}")
                }
            } else {
                // The document does not exist, set it with the new observation
                userObservationsRef.set(mapOf("observations" to listOf(observationMap))).addOnSuccessListener {
                    showToast("Observation saved successfully")
                     incrementObservationCount() // If you have this method, call it here
                }.addOnFailureListener { e ->
                    showToast("Failed to create document: ${e.message}")
                }
            }
        }.addOnFailureListener { e ->
            showToast("Failed to check document: ${e.message}")
        }
    }
    private fun incrementObservationCount() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return // If user is not logged in, return early

        val challengeRef = FirebaseFirestore.getInstance().collection("challenges").document(currentUserId)
        FirebaseFirestore.getInstance().runTransaction { transaction ->
            val snapshot = transaction.get(challengeRef)
            val isActive = snapshot.getBoolean("isActive") ?: false // Get the current 'isActive' status
            val currentCount = snapshot.getLong("observationCounter") ?: 0 // Get the current observation count

            if (isActive) {
                // If the challenge is active, increment the observation counter
                transaction.update(challengeRef, "observationCounter", currentCount + 1)

            } else {
                // If the challenge is not active, throw an exception to abort the transaction
                throw FirebaseFirestoreException("Challenge is not active.",
                    FirebaseFirestoreException.Code.ABORTED)
            }
            null // Return null to indicate success
        }
            .addOnSuccessListener {
                // Handle success (e.g., show a toast or update UI)
                showToast("Added to counter!")
                //checkChallengeCompletion(currentUserId)
            }
            .addOnFailureListener { e ->
                // Handle failure (e.g., show a toast or error message)
                showToast("Failed to add observation: ${e.message}")
            }
    }

    private fun checkChallengeCompletion(userId: String) {
        // Logic to check if the challenge is completed
        // If completed, call awardBadge(userId)
    }

    private fun awardBadge(userId: String) {
        // Logic to award a badge, such as updating a 'badges' collection
        showToast("Challenge completed! Badge awarded!")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
