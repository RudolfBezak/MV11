package com.example.mv11

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {
    @POST("user/create.php")
    suspend fun registerUser(
        @retrofit2.http.Header("x-apikey") apiKey: String,
        @Body userInfo: UserRegistration
    ): Response<RegistrationResponse>

    @POST("user/login.php")
    suspend fun loginUser(
        @retrofit2.http.Header("x-apikey") apiKey: String,
        @Body loginInfo: UserLogin
    ): Response<RegistrationResponse>

    @retrofit2.http.GET("user")
    suspend fun listGeofence(
        @retrofit2.http.Header("x-apikey") apiKey: String
    ): Response<List<UserResponse>>

    @POST("users/logout")
    suspend fun logout(
        @retrofit2.http.Header("x-apikey") apiKey: String,
        @retrofit2.http.Header("Authorization") accessToken: String
    ): Response<Void>

    companion object {
        fun create(): ApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://zadanie.mpage.sk/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}

