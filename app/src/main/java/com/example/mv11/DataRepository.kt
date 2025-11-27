package com.example.mv11

import android.content.Context
import android.util.Log
import java.io.IOException

class DataRepository private constructor(
    private val service: ApiService,
    private val cache: LocalCache
) {
    companion object {
        const val TAG = "DataRepository"
        const val API_KEY = "c95332ee022df8c953ce470261efc695ecf3e784"

        @Volatile
        private var INSTANCE: DataRepository? = null
        private val lock = Any()

        fun getInstance(context: Context): DataRepository =
            INSTANCE ?: synchronized(lock) {
                INSTANCE ?: DataRepository(
                    ApiService.create(),
                    LocalCache(AppRoomDatabase.getInstance(context).appDao())
                ).also { INSTANCE = it }
            }
    }

    suspend fun apiRegisterUser(username: String, email: String, password: String): Pair<String, User?> {
        if (username.isEmpty()) {
            Log.e(TAG, "Username is empty")
            return Pair("Username can not be empty", null)
        }
        if (email.isEmpty()) {
            Log.e(TAG, "Email is empty")
            return Pair("Email can not be empty", null)
        }
        if (password.isEmpty()) {
            Log.e(TAG, "Password is empty")
            return Pair("Password can not be empty", null)
        }
        try {
            Log.d(TAG, "Sending registration request for user: $username, email: $email")
            Log.d(TAG, "Using API key: $API_KEY")
            val response = service.registerUser(API_KEY, UserRegistration(username, email, password))
            Log.d(TAG, "Response code: ${response.code()}")
            Log.d(TAG, "Response message: ${response.message()}")
            Log.d(TAG, "Response isSuccessful: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "Response body: $body")
                
                if (body != null) {
                    Log.d(TAG, "User created successfully: uid=${body.uid}")
                    return Pair("", User(username, email, body.uid, body.access, body.refresh))
                } else {
                    Log.e(TAG, "Response body is null")
                    return Pair("Server returned empty response", null)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Request failed with code: ${response.code()}, error: $errorBody")
                return Pair("Failed to create user: ${response.message()}", null)
            }
        } catch (ex: IOException) {
            Log.e(TAG, "IOException: ${ex.message}", ex)
            ex.printStackTrace()
            return Pair("Check internet connection. Failed to create user.", null)
        } catch (ex: Exception) {
            Log.e(TAG, "Exception: ${ex.message}", ex)
            ex.printStackTrace()
            return Pair("Error: ${ex.message}", null)
        }
    }

    suspend fun apiLoginUser(email: String, password: String): Pair<String, User?> {
        if (email.isEmpty()) {
            Log.e(TAG, "Email is empty")
            return Pair("Email can not be empty", null)
        }
        if (password.isEmpty()) {
            Log.e(TAG, "Password is empty")
            return Pair("Password can not be empty", null)
        }
        try {
            Log.d(TAG, "Sending login request for email: $email")
            Log.d(TAG, "Using API key: $API_KEY")
            val response = service.loginUser(API_KEY, UserLogin(email, password))
            Log.d(TAG, "Response code: ${response.code()}")
            Log.d(TAG, "Response message: ${response.message()}")
            Log.d(TAG, "Response isSuccessful: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "Response body: $body")
                
                if (body != null) {
                    Log.d(TAG, "Login successful: uid=${body.uid}")
                    Log.d(TAG, "Access token from API: ${body.access}, length: ${body.access.length}")
                    Log.d(TAG, "Refresh token from API: ${body.refresh}, length: ${body.refresh.length}")
                    val username = email.substringBefore("@")
                    return Pair("", User(username, email, body.uid, body.access, body.refresh))
                } else {
                    Log.e(TAG, "Response body is null")
                    return Pair("Server returned empty response", null)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Request failed with code: ${response.code()}, error: $errorBody")
                return Pair("Failed to login: ${response.message()}", null)
            }
        } catch (ex: IOException) {
            Log.e(TAG, "IOException: ${ex.message}", ex)
            ex.printStackTrace()
            return Pair("Check internet connection. Failed to login.", null)
        } catch (ex: Exception) {
            Log.e(TAG, "Exception: ${ex.message}", ex)
            ex.printStackTrace()
            return Pair("Error: ${ex.message}", null)
        }
    }

    suspend fun apiListGeofence(useMockData: Boolean = false): String {
        if (useMockData) {
            Log.d(TAG, "Using mock data for testing")
            val mockUsers = MockDataHelper.getMockUsers()
            cache.insertUserItems(mockUsers)
            Log.d(TAG, "Mock users saved to database: ${mockUsers.size}")
            return ""
        }
        
        try {
            Log.d(TAG, "Fetching users from API")
            val response = service.listGeofence(API_KEY)
            Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful) {
                response.body()?.let { userResponses ->
                    Log.d(TAG, "Received ${userResponses.size} users from API")
                    val users = userResponses.map {
                        UserEntity(
                            it.uid, it.name, it.updated,
                            it.lat, it.lon, it.radius, it.photo
                        )
                    }
                    cache.insertUserItems(users)
                    Log.d(TAG, "Users saved to database")
                    return ""
                }
            }

            Log.e(TAG, "Failed to load users: ${response.message()} (code: ${response.code()})")
            if (response.code() == 404) {
                Log.w(TAG, "API endpoint not found, using mock data as fallback")
                return apiListGeofence(useMockData = true)
            }
            return "Failed to load users"
        } catch (ex: IOException) {
            Log.e(TAG, "IOException: ${ex.message}", ex)
            ex.printStackTrace()
            return "Check internet connection. Failed to load users."
        } catch (ex: Exception) {
            Log.e(TAG, "Exception: ${ex.message}", ex)
            ex.printStackTrace()
        }
        return "Fatal error. Failed to load users."
    }

    suspend fun apiLogout(accessToken: String): Pair<String, Boolean> {
        try {
            Log.d(TAG, "Sending logout request")
            Log.d(TAG, "Using API key: $API_KEY")
            val authHeader = "Bearer $accessToken"
            val response = service.logout(API_KEY, authHeader)
            Log.d(TAG, "Logout response code: ${response.code()}")
            Log.d(TAG, "Logout response message: ${response.message()}")
            Log.d(TAG, "Logout response isSuccessful: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                Log.d(TAG, "Logout successful")
                return Pair("", true)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Logout failed with code: ${response.code()}, error: $errorBody")
                return Pair("Failed logout API, still logging out: ${response.message()}", true)
            }
        } catch (ex: IOException) {
            Log.e(TAG, "IOException: ${ex.message}", ex)
            ex.printStackTrace()
            return Pair("Check internet connection. Failed to logout.", false)
        } catch (ex: Exception) {
            Log.e(TAG, "Exception: ${ex.message}", ex)
            ex.printStackTrace()
            return Pair("Error: ${ex.message}", false)
        }
    }

    fun getUsers() = cache.getUsers()
}

