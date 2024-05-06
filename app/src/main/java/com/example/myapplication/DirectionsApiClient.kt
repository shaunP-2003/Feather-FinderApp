package com.example.myapplication

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object DirectionsApiClient {
    private const val BASE_URL = "https://maps.googleapis.com/"

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val directionsService: DirectionsApiService = retrofit.create(DirectionsApiService::class.java)
}
