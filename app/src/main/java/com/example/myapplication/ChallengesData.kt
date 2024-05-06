package com.example.myapplication

import kotlin.Double
import kotlin.String

data class ChallengesData (
    val id: String, // You would use the document ID from Firestore as this field when fetching documents
    val isActive: Boolean,
    val observationCounter: Int
)