package com.example.mv11

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    /**
     * Registrácia nového používateľa.
     * Tento endpoint nevyžaduje autentifikáciu (nie je chránený).
     */
    @POST("user/create.php")
    suspend fun registerUser(
        @Header("x-apikey") apiKey: String,
        @Body userInfo: UserRegistration
    ): Response<RegistrationResponse>

    /**
     * Získa zoznam používateľov (geofence).
     * Token sa pridá automaticky cez AuthInterceptor.
     */
    @GET("user/list.php")
    suspend fun listGeofence(): Response<List<UserResponse>>

    /**
     * Získa údaje o používateľovi z API.
     * 
     * Token sa pridá automaticky cez AuthInterceptor.
     * Ak token expiroval, TokenAuthenticator ho automaticky obnoví.
     * 
     * @param id - uid používateľa ktorého chceme získať
     * @return Response<UserResponse> - odpoveď zo servera
     */
    @GET("user/get.php")
    suspend fun getUser(
        @Query("id") id: String
    ): Response<UserResponse>

    /**
     * Obnoví access token pomocou refresh tokenu (suspend verzia).
     * Používa sa v normálnych coroutine volaniach.
     * 
     * @param refreshInfo - RefreshTokenRequest obsahujúci refresh token
     * @return Response<RefreshTokenResponse> - nové access a refresh tokeny
     */
    @POST("user/refresh.php")
    suspend fun refreshToken(
        @Body refreshInfo: RefreshTokenRequest
    ): Response<RefreshTokenResponse>

    /**
     * Obnoví access token pomocou refresh tokenu (blocking verzia).
     * Používa sa v TokenAuthenticator (musí byť synchronný).
     * 
     * @param userId - uid aktuálneho používateľa (pre x-user header)
     * @param refreshInfo - RefreshTokenRequest obsahujúci refresh token
     * @return Call<RefreshTokenResponse> - synchronný call pre execute()
     */
    @POST("user/refresh.php")
    fun refreshTokenBlocking(
        @Header("x-user") userId: String,
        @Body refreshInfo: RefreshTokenRequest
    ): Call<RefreshTokenResponse>

    /**
     * Odosle aktuálnu polohu používateľa na server.
     * Token sa pridá automaticky cez AuthInterceptor.
     * 
     * @param geofenceUpdate - GeofenceUpdateRequest obsahujúci lat, lon, radius
     * @return Response<Unit> - odpoveď zo servera
     */
    @POST("geofence/update.php")
    suspend fun updateGeofence(
        @Body geofenceUpdate: GeofenceUpdateRequest
    ): Response<Unit>

    /**
     * Odstráni polohu používateľa zo servera.
     * Token sa pridá automaticky cez AuthInterceptor.
     * 
     * @return Response<Unit> - odpoveď zo servera
     */
    @DELETE("geofence/update.php")
    suspend fun deleteGeofence(): Response<Unit>

    /**
     * Získa zoznam všetkých polôh používateľov (vrátane vlastnej).
     * Token sa pridá automaticky cez AuthInterceptor.
     * 
     * @return Response<List<GeofenceResponse>> - zoznam polôh používateľov
     */
    @GET("geofence/list.php")
    suspend fun listGeofence(): Response<List<GeofenceResponse>>

    companion object {
        /**
         * Vytvorí ApiService inštanciu s automatickou autentifikáciou.
         * 
         * @param context - kontext aplikácie (potrebný pre SharedPreferences)
         * @return ApiService inštancia s AuthInterceptor a TokenAuthenticator
         */
        fun create(context: Context): ApiService {
            // Logging interceptor - loguje všetky HTTP requesty a odpovede
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // AuthInterceptor - automaticky pridá Authorization header a API key
            val authInterceptor = AuthInterceptor(context)

            // TokenAuthenticator - automaticky obnoví token pri 401 chybe
            val tokenAuthenticator = TokenAuthenticator(context)

            // Vytvor OkHttpClient s interceptormi
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)  // Logging (prvý - vidí request pred zmenou)
                .addInterceptor(authInterceptor)    // Auth (druhý - pridá tokeny)
                .authenticator(tokenAuthenticator)  // Authenticator (obnoví token pri 401)
                .build()

            // Vytvor Retrofit inštanciu
            val retrofit = Retrofit.Builder()
                .baseUrl("https://zadanie.mpage.sk/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}

