package com.example.myapplication

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://api.ebird.org/v2/"

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("X-eBirdApiToken", "e8ppp9jdu5h0")
                .build()
            chain.proceed(newRequest)
        }
        .build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)  // Add the custom client to add the header to each request
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

