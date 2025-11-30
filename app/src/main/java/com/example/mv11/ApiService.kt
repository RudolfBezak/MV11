package com.example.mv11

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.Part

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

    @GET("geofence/list.php")
    suspend fun listGeofence(
        @retrofit2.http.Header("x-apikey") apiKey: String,
        @retrofit2.http.Header("Authorization") accessToken: String
    ): Response<GeofenceListResponse>

    @POST("users/logout")
    suspend fun logout(
        @retrofit2.http.Header("x-apikey") apiKey: String,
        @retrofit2.http.Header("Authorization") accessToken: String
    ): Response<Void>

    @POST("user/reset.php")
    suspend fun resetPassword(
        @retrofit2.http.Header("x-apikey") apiKey: String,
        @Body resetInfo: PasswordResetRequest
    ): Response<PasswordResetResponse>

    @POST("user/password.php")
    suspend fun changePassword(
        @retrofit2.http.Header("x-apikey") apiKey: String,
        @retrofit2.http.Header("Authorization") accessToken: String,
        @Body passwordInfo: PasswordChangeRequest
    ): Response<PasswordChangeResponse>

    @POST("geofence/update.php")
    suspend fun updateGeofence(
        @retrofit2.http.Header("x-apikey") apiKey: String,
        @retrofit2.http.Header("Authorization") accessToken: String,
        @Body geofenceInfo: GeofenceUpdateRequest
    ): Response<GeofenceUpdateResponse>

    @DELETE("geofence/update.php")
    suspend fun deleteGeofence(
        @retrofit2.http.Header("x-apikey") apiKey: String,
        @retrofit2.http.Header("Authorization") accessToken: String
    ): Response<GeofenceUpdateResponse>

    @GET("user/get.php")
    suspend fun getUserProfile(
        @retrofit2.http.Header("x-apikey") apiKey: String,
        @retrofit2.http.Header("Authorization") accessToken: String,
        @Query("id") userId: String
    ): Response<UserProfileResponse>

    @POST("user/refresh.php")
    suspend fun refreshToken(
        @retrofit2.http.Header("x-apikey") apiKey: String,
        @retrofit2.http.Header("x-user") userId: String,
        @Body refreshRequest: RefreshTokenRequest
    ): Response<RefreshTokenResponse>

    companion object {
        fun create(context: Context? = null): ApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val clientBuilder = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
            
            // Add auth interceptor if context is available
            if (context != null) {
                clientBuilder.addInterceptor(AuthInterceptor(context))
            }

            val client = clientBuilder.build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://zadanie.mpage.sk/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}

interface UploadApiService {
    @Multipart
    @POST("user/photo.php")
    suspend fun uploadPhoto(
        @retrofit2.http.Header("x-apikey") apiKey: String,
        @retrofit2.http.Header("Authorization") accessToken: String,
        @Part image: MultipartBody.Part
    ): Response<PhotoUploadResponse>

    @DELETE("user/photo.php")
    suspend fun deletePhoto(
        @retrofit2.http.Header("x-apikey") apiKey: String,
        @retrofit2.http.Header("Authorization") accessToken: String
    ): Response<PhotoUploadResponse>

    companion object {
        fun create(context: Context? = null): UploadApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val clientBuilder = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
            
            // Add auth interceptor if context is available
            if (context != null) {
                clientBuilder.addInterceptor(AuthInterceptor(context))
            }

            val client = clientBuilder.build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://upload.mcomputing.eu/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(UploadApiService::class.java)
        }
    }
}

