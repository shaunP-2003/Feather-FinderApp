package com.example.myapplication

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface MyApiService {
    @GET("data/obs/geo/recent")
    fun getHotspots(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double
    ): Call<List<HotSpot>>
}



