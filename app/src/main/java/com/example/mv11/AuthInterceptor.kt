package com.example.mv11

import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class AuthInterceptor(private val context: Context) : Interceptor {
    companion object {
        private const val TAG = "AuthInterceptor"
        private const val API_KEY = "c95332ee022df8c953ce470261efc695ecf3e784"
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val user = PreferenceData.getInstance().getUser(context)
        
        // Build request with headers
        val requestBuilder = originalRequest.newBuilder()
        
        // Add x-apikey if not already present
        if (originalRequest.header("x-apikey") == null) {
            requestBuilder.header("x-apikey", API_KEY)
        }
        
        // Add Authorization header if user is logged in and header not already present
        if (user != null && user.access.isNotEmpty() && originalRequest.header("Authorization") == null) {
            requestBuilder.header("Authorization", "Bearer ${user.access}")
        }
        
        val authorizedRequest = requestBuilder.build()
        var response = chain.proceed(authorizedRequest)

        // If we get 401 Unauthorized, try to refresh the token
        if (response.code == 401 && user != null && user.refresh.isNotEmpty()) {
            Log.d(TAG, "Received 401 Unauthorized, attempting to refresh token")
            
            // Try to refresh the token
            val refreshSuccess = refreshAccessToken(user)
            
            if (refreshSuccess) {
                // Get updated user with new access token
                val updatedUser = PreferenceData.getInstance().getUser(context)
                if (updatedUser != null && updatedUser.access.isNotEmpty()) {
                    // Close the original response
                    response.close()
                    
                    // Retry the original request with new token
                    val retryRequestBuilder = originalRequest.newBuilder()
                    
                    // Remove old Authorization header if exists and add new one
                    retryRequestBuilder.removeHeader("Authorization")
                    retryRequestBuilder.header("Authorization", "Bearer ${updatedUser.access}")
                    
                    // Ensure x-apikey is present
                    if (retryRequestBuilder.build().header("x-apikey") == null) {
                        retryRequestBuilder.header("x-apikey", API_KEY)
                    }
                    
                    val retryRequest = retryRequestBuilder.build()
                    
                    Log.d(TAG, "Retrying request with refreshed token")
                    return chain.proceed(retryRequest)
                }
            } else {
                // Refresh failed, logout user
                Log.e(TAG, "Token refresh failed, logging out user")
                logoutUser()
                // Return the original 401 response (it's already closed, but that's ok)
                return response
            }
        }

        return response
    }

    private fun refreshAccessToken(user: User): Boolean {
        return try {
            val repository = DataRepository.getInstance(context)
            val result = runBlocking {
                repository.apiRefreshToken(user.uid, user.refresh)
            }
            
            if (result.second != null) {
                val refreshResponse = result.second!!
                // Update user with new tokens
                val updatedUser = User(
                    name = user.name,
                    email = user.email,
                    uid = refreshResponse.uid,
                    access = refreshResponse.access,
                    refresh = refreshResponse.refresh
                )
                PreferenceData.getInstance().putUser(context, updatedUser)
                Log.d(TAG, "Token refreshed successfully")
                true
            } else {
                Log.e(TAG, "Token refresh failed: ${result.first}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during token refresh: ${e.message}", e)
            false
        }
    }

    private fun logoutUser() {
        PreferenceData.getInstance().clearData(context)
        PreferenceData.getInstance().clearCurrentLocation(context)
        Log.d(TAG, "User logged out due to token refresh failure")
    }
}

