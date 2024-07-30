package com.example.send_angle2

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import okhttp3.ResponseBody

interface ApiService {
    @GET("servo")
    suspend fun setServoAngles(
        @Query("angle1") angle1: Int,
        @Query("angle2") angle2: Int
    ): ResponseBody
}

object RetrofitClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.4.31/")  // Replace with the IP address of your ESP32
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
