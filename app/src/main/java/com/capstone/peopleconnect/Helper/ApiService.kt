package com.capstone.peopleconnect.Helper

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @POST("/update-clicks")
    fun updateClicks(@Body clickData: ClickData): Call<Map<String, String>>

    @GET("/get-recommendations/{user_id}")
    fun getRecommendations(@Path("user_id") userId: String): Call<Map<String, Any>>
}

data class ClickData(
    val user_id: String,
    val clicked_category: String
)
