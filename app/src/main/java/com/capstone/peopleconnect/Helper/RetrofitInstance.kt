package com.capstone.peopleconnect.Helper

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Change from class to object for Singleton pattern
object RetrofitInstance {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://algp-api-server.onrender.com/") // Your FastAPI URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Lazy initialization of the API service interface
    val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
