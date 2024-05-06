package com.example.myapplication


data class BirdHotspotArea(
    val id: String,
    val lat: Double,
    val lng: Double,
    val radius: Float, // in meters
    val birdSpecies: List<String> // List of bird species in this area
)