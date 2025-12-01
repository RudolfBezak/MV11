package com.example.mv11

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class DataRepository private constructor(
    private val service: ApiService,
    private val uploadService: UploadApiService,
    private val cache: LocalCache,
    private val context: Context
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
                    ApiService.create(context),
                    UploadApiService.create(context),
                    LocalCache(AppRoomDatabase.getInstance(context).appDao()),
                    context
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
                    Log.d(TAG, "Registration response: uid=${body.uid}, access=${body.access}, refresh=${body.refresh}")
                    
                    // Check if username already exists (uid = -1)
                    if (body.uid == "-1") {
                        Log.e(TAG, "Username already exists")
                        return Pair("Používateľské meno už existuje. Vyberte si iné používateľské meno.", null)
                    }
                    
                    // Check if email already exists (uid = -2)
                    if (body.uid == "-2") {
                        Log.e(TAG, "Email already exists")
                        return Pair("Email už existuje. Vyberte si iný email.", null)
                    }
                    
                    // Check if access token is empty (registration failed for other reason)
                    if (body.access.isEmpty()) {
                        Log.e(TAG, "Access token is empty after registration")
                        return Pair("Registrácia zlyhala. Skúste to znova.", null)
                    }
                    
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

    suspend fun apiLoginUser(nameOrEmail: String, password: String): Pair<String, User?> {
        if (nameOrEmail.isEmpty()) {
            Log.e(TAG, "Name/Email is empty")
            return Pair("Meno alebo email nemôže byť prázdne", null)
        }
        if (password.isEmpty()) {
            Log.e(TAG, "Password is empty")
            return Pair("Heslo nemôže byť prázdne", null)
        }
        try {
            Log.d(TAG, "Sending login request for name/email: $nameOrEmail")
            Log.d(TAG, "Using API key: $API_KEY")
            val response = service.loginUser(API_KEY, UserLogin(nameOrEmail, password))
            Log.d(TAG, "Response code: ${response.code()}")
            Log.d(TAG, "Response message: ${response.message()}")
            Log.d(TAG, "Response isSuccessful: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "Response body: $body")
                
                if (body != null) {
                    Log.d(TAG, "Login response: uid=${body.uid}, access=${body.access}, refresh=${body.refresh}")
                    
                    // Check if login failed (uid = -1 means wrong username/email or password)
                    if (body.uid == "-1") {
                        Log.e(TAG, "Login failed: wrong username/email or password")
                        return Pair("Meno alebo heslo je nesprávne", null)
                    }
                    
                    // Check if access token is empty (login failed for other reason)
                    if (body.access.isEmpty()) {
                        Log.e(TAG, "Access token is empty after login")
                        return Pair("Prihlásenie zlyhalo. Skúste to znova.", null)
                    }
                    
                    Log.d(TAG, "Login successful: uid=${body.uid}")
                    Log.d(TAG, "Access token from API: ${body.access}, length: ${body.access.length}")
                    Log.d(TAG, "Refresh token from API: ${body.refresh}, length: ${body.refresh.length}")
                    
                    // Use nameOrEmail as both name and email (API accepts both)
                    val username = if (nameOrEmail.contains("@")) {
                        nameOrEmail.substringBefore("@")
                    } else {
                        nameOrEmail
                    }
                    return Pair("", User(username, nameOrEmail, body.uid, body.access, body.refresh))
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

    suspend fun apiListGeofence(accessToken: String): Triple<String, Boolean, GeofenceMe?> {
        if (accessToken.isEmpty()) {
            Log.e(TAG, "Access token is empty")
            return Triple("Access token nemôže byť prázdny", false, null)
        }
        
        try {
            Log.d(TAG, "Fetching geofence list from API")
            Log.d(TAG, "Using API key: $API_KEY")
            val authHeader = "Bearer $accessToken"
            val response = service.listGeofence(API_KEY, authHeader)
            Log.d(TAG, "Response code: ${response.code()}")
            Log.d(TAG, "Response message: ${response.message()}")
            Log.d(TAG, "Response isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "Response body: $body")

                if (body != null) {
                    // Check if geofence is enabled based on response structure
                    if (body.me != null) {
                        // "me" object exists - geofence is enabled
                        PreferenceData.getInstance().setLocationSharingEnabled(context, true)
                        Log.d(TAG, "Geofence is enabled (me object present)")
                        
                        // Save "me" object coordinates if available
                        val lat = body.me.lat.toDoubleOrNull() ?: 0.0
                        val lon = body.me.lon.toDoubleOrNull() ?: 0.0
                        val radius = body.me.radius.toDoubleOrNull() ?: 100.0
                        if (lat != 0.0 && lon != 0.0) {
                            PreferenceData.getInstance().setCurrentLocation(
                                context,
                                lat,
                                lon,
                                radius
                            )
                            Log.d(TAG, "Saved current location: lat=$lat, lon=$lon, radius=$radius")
                        }
                    } else {
                        // No "me" object - geofence is disabled
                        PreferenceData.getInstance().setLocationSharingEnabled(context, false)
                        Log.d(TAG, "Geofence is disabled (no me object)")
                    }

                    if (body.list.isEmpty()) {
                        Log.w(TAG, "Geofence list is empty - user needs to enable geofence")
                        cache.deleteUserItems() // Clear database
                        return Triple("MUSIS_SI_ZAPNUT_GEOFENCE", false, body.me)
                    }

                    Log.d(TAG, "Received ${body.list.size} users from API")
                    // Clear database before inserting new data
                    cache.deleteUserItems()
                    
                    // Get lat/lon from "me" object if available
                    val meLat = body.me?.lat?.toDoubleOrNull() ?: 0.0
                    val meLon = body.me?.lon?.toDoubleOrNull() ?: 0.0
                    val meUid = body.me?.uid
                    
                    // Note: lat/lon are not in list items, only in "me" object
                    // Use lat/lon from "me" object if uid matches, otherwise set to 0.0
                    val users = body.list.map {
                        val lat = if (it.uid == meUid && meLat != 0.0) meLat else 0.0
                        val lon = if (it.uid == meUid && meLon != 0.0) meLon else 0.0
                        UserEntity(
                            it.uid,
                            it.name,
                            it.updated,
                            lat,
                            lon,
                            it.radius.toDoubleOrNull() ?: 0.0,
                            it.photo
                        )
                    }
                    cache.insertUserItems(users)
                    Log.d(TAG, "Users saved to database (old data cleared)")
                    return Triple("", true, body.me)
                } else {
                    Log.e(TAG, "Response body is null")
                    return Triple("Server returned empty response", false, null)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Request failed with code: ${response.code()}, error: $errorBody")
                return Triple("Failed to load users: ${response.message()}", false, null)
            }
        } catch (ex: IOException) {
            Log.e(TAG, "IOException: ${ex.message}", ex)
            ex.printStackTrace()
            return Triple("Skontrolujte internetové pripojenie. Nepodarilo sa načítať používateľov.", false, null)
        } catch (ex: Exception) {
            Log.e(TAG, "Exception: ${ex.message}", ex)
            ex.printStackTrace()
            return Triple("Chyba: ${ex.message}", false, null)
        }
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

    suspend fun getUsersSync(): List<UserEntity?> = cache.getUsersSync()

    suspend fun clearDatabase() {
        cache.deleteUserItems()
    }

    suspend fun apiResetPassword(email: String): Pair<String, Boolean> {
        if (email.isEmpty()) {
            Log.e(TAG, "Email is empty")
            return Pair("Email nemôže byť prázdny", false)
        }
        try {
            Log.d(TAG, "Sending password reset request for email: $email")
            Log.d(TAG, "Using API key: $API_KEY")
            val response = service.resetPassword(API_KEY, PasswordResetRequest(email))
            Log.d(TAG, "Response code: ${response.code()}")
            Log.d(TAG, "Response message: ${response.message()}")
            Log.d(TAG, "Response isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "Response body: $body")

                if (body != null && body.status == "success") {
                    Log.d(TAG, "Password reset email sent successfully")
                    return Pair("", true)
                } else {
                    val errorMessage = body?.message ?: "Nepodarilo sa odoslať email"
                    Log.e(TAG, "Password reset failed: $errorMessage")
                    return Pair(errorMessage, false)
                }
            } else {
                val body = response.errorBody()?.string()
                Log.e(TAG, "Request failed with code: ${response.code()}, error: $body")
                val errorBody = try {
                    val gson = com.google.gson.Gson()
                    val errorResponse = gson.fromJson(body, PasswordResetResponse::class.java)
                    errorResponse.message ?: "Nepodarilo sa odoslať email"
                } catch (e: Exception) {
                    "Nepodarilo sa odoslať email"
                }
                return Pair(errorBody, false)
            }
        } catch (ex: IOException) {
            Log.e(TAG, "IOException: ${ex.message}", ex)
            ex.printStackTrace()
            return Pair("Skontrolujte internetové pripojenie. Nepodarilo sa odoslať email.", false)
        } catch (ex: Exception) {
            Log.e(TAG, "Exception: ${ex.message}", ex)
            ex.printStackTrace()
            return Pair("Chyba: ${ex.message}", false)
        }
    }

    suspend fun apiChangePassword(accessToken: String, oldPassword: String, newPassword: String): Pair<String, Boolean> {
        if (oldPassword.isEmpty()) {
            Log.e(TAG, "Old password is empty")
            return Pair("Staré heslo nemôže byť prázdne", false)
        }
        if (newPassword.isEmpty()) {
            Log.e(TAG, "New password is empty")
            return Pair("Nové heslo nemôže byť prázdne", false)
        }
        if (accessToken.isEmpty()) {
            Log.e(TAG, "Access token is empty")
            return Pair("Access token nemôže byť prázdny", false)
        }
        try {
            Log.d(TAG, "Sending password change request")
            Log.d(TAG, "Using API key: $API_KEY")
            val authHeader = "Bearer $accessToken"
            val response = service.changePassword(API_KEY, authHeader, PasswordChangeRequest(oldPassword, newPassword))
            Log.d(TAG, "Response code: ${response.code()}")
            Log.d(TAG, "Response message: ${response.message()}")
            Log.d(TAG, "Response isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "Response body: $body")

                if (body != null && body.status == "success") {
                    Log.d(TAG, "Password changed successfully")
                    return Pair("", true)
                } else {
                    Log.e(TAG, "Password change failed: status=${body?.status}")
                    return Pair("Nepodarilo sa zmeniť heslo", false)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Request failed with code: ${response.code()}, error: $errorBody")
                return Pair("Nepodarilo sa zmeniť heslo: ${response.message()}", false)
            }
        } catch (ex: IOException) {
            Log.e(TAG, "IOException: ${ex.message}", ex)
            ex.printStackTrace()
            return Pair("Skontrolujte internetové pripojenie. Nepodarilo sa zmeniť heslo.", false)
        } catch (ex: Exception) {
            Log.e(TAG, "Exception: ${ex.message}", ex)
            ex.printStackTrace()
            return Pair("Chyba: ${ex.message}", false)
        }
    }

    suspend fun apiUpdateGeofence(accessToken: String, lat: Double, lon: Double, radius: Double): Pair<String, Boolean> {
        if (accessToken.isEmpty()) {
            Log.e(TAG, "Access token is empty")
            return Pair("Access token nemôže byť prázdny", false)
        }
        try {
            Log.d(TAG, "Sending geofence update request: lat=$lat, lon=$lon, radius=$radius")
            Log.d(TAG, "Using API key: $API_KEY")
            val authHeader = "Bearer $accessToken"
            val response = service.updateGeofence(API_KEY, authHeader, GeofenceUpdateRequest(lat, lon, radius))
            Log.d(TAG, "Response code: ${response.code()}")
            Log.d(TAG, "Response message: ${response.message()}")
            Log.d(TAG, "Response isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "Response body: $body")

                if (body != null && body.success) {
                    Log.d(TAG, "Geofence updated successfully")
                    return Pair("", true)
                } else {
                    Log.e(TAG, "Geofence update failed: success=${body?.success}")
                    return Pair("Nepodarilo sa aktualizovať polohu", false)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Request failed with code: ${response.code()}, error: $errorBody")
                return Pair("Nepodarilo sa aktualizovať polohu: ${response.message()}", false)
            }
        } catch (ex: IOException) {
            Log.e(TAG, "IOException: ${ex.message}", ex)
            ex.printStackTrace()
            return Pair("Skontrolujte internetové pripojenie. Nepodarilo sa aktualizovať polohu.", false)
        } catch (ex: Exception) {
            Log.e(TAG, "Exception: ${ex.message}", ex)
            ex.printStackTrace()
            return Pair("Chyba: ${ex.message}", false)
        }
    }

    suspend fun apiDeleteGeofence(accessToken: String): Pair<String, Boolean> {
        if (accessToken.isEmpty()) {
            Log.e(TAG, "Access token is empty")
            return Pair("Access token nemôže byť prázdny", false)
        }
        try {
            Log.d(TAG, "Sending geofence delete request")
            Log.d(TAG, "Using API key: $API_KEY")
            val authHeader = "Bearer $accessToken"
            val response = service.deleteGeofence(API_KEY, authHeader)
            Log.d(TAG, "Response code: ${response.code()}")
            Log.d(TAG, "Response message: ${response.message()}")
            Log.d(TAG, "Response isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "Response body: $body")

                if (body != null && body.success) {
                    Log.d(TAG, "Geofence deleted successfully")
                    return Pair("", true)
                } else {
                    Log.e(TAG, "Geofence delete failed: success=${body?.success}")
                    return Pair("Nepodarilo sa odstrániť polohu", false)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Request failed with code: ${response.code()}, error: $errorBody")
                return Pair("Nepodarilo sa odstrániť polohu: ${response.message()}", false)
            }
        } catch (ex: IOException) {
            Log.e(TAG, "IOException: ${ex.message}", ex)
            ex.printStackTrace()
            return Pair("Skontrolujte internetové pripojenie. Nepodarilo sa odstrániť polohu.", false)
        } catch (ex: Exception) {
            Log.e(TAG, "Exception: ${ex.message}", ex)
            ex.printStackTrace()
            return Pair("Chyba: ${ex.message}", false)
        }
    }

    suspend fun apiGetUserProfile(accessToken: String, userId: String): Pair<String, UserProfileResponse?> {
        if (accessToken.isEmpty()) {
            Log.e(TAG, "Access token is empty")
            return Pair("Access token nemôže byť prázdny", null)
        }

        try {
            Log.d(TAG, "Fetching user profile for userId: $userId")
            val authHeader = "Bearer $accessToken"
            val response = service.getUserProfile(API_KEY, authHeader, userId)
            Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "Response body: $body")
                if (body != null) {
                    return Pair("", body)
                } else {
                    return Pair("Server returned empty response", null)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Request failed with code: ${response.code()}, error: $errorBody")
                return Pair("Failed to load profile: ${response.message()}", null)
            }
        } catch (ex: IOException) {
            Log.e(TAG, "IOException: ${ex.message}", ex)
            return Pair("Network error: ${ex.message}", null)
        }
    }

    suspend fun apiUploadPhoto(accessToken: String, imageFile: File): Pair<String, PhotoUploadResponse?> {
        if (accessToken.isEmpty()) {
            Log.e(TAG, "Access token is empty")
            return Pair("Access token nemôže byť prázdny", null)
        }

        try {
            Log.d(TAG, "Uploading photo: ${imageFile.absolutePath}")
            val authHeader = "Bearer $accessToken"
            
            // Create request body for image file
            val requestFile = imageFile.asRequestBody("image/jpeg".toMediaType())
            val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
            
            val response = uploadService.uploadPhoto(API_KEY, authHeader, imagePart)
            Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "Response body: $body")
                if (body != null) {
                    return Pair("", body)
                } else {
                    return Pair("Server returned empty response", null)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Request failed with code: ${response.code()}, error: $errorBody")
                return Pair("Failed to upload photo: ${response.message()}", null)
            }
        } catch (ex: IOException) {
            Log.e(TAG, "IOException: ${ex.message}", ex)
            return Pair("Network error: ${ex.message}", null)
        }
    }

    suspend fun apiDeletePhoto(accessToken: String): Pair<String, PhotoUploadResponse?> {
        if (accessToken.isEmpty()) {
            Log.e(TAG, "Access token is empty")
            return Pair("Access token nemôže byť prázdny", null)
        }

        try {
            Log.d(TAG, "Deleting photo")
            val authHeader = "Bearer $accessToken"
            val response = uploadService.deletePhoto(API_KEY, authHeader)
            Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "Response body: $body")
                if (body != null) {
                    return Pair("", body)
                } else {
                    return Pair("Server returned empty response", null)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Request failed with code: ${response.code()}, error: $errorBody")
                return Pair("Failed to delete photo: ${response.message()}", null)
            }
        } catch (ex: IOException) {
            Log.e(TAG, "IOException: ${ex.message}", ex)
            return Pair("Network error: ${ex.message}", null)
        }
    }

    suspend fun apiRefreshToken(userId: String, refreshToken: String): Pair<String, RefreshTokenResponse?> {
        if (refreshToken.isEmpty()) {
            Log.e(TAG, "Refresh token is empty")
            return Pair("Refresh token nemôže byť prázdny", null)
        }

        try {
            Log.d(TAG, "Refreshing access token for userId: $userId")
            val response = service.refreshToken(API_KEY, userId, RefreshTokenRequest(refreshToken))
            Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "Response body: $body")
                if (body != null) {
                    return Pair("", body)
                } else {
                    return Pair("Server returned empty response", null)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Request failed with code: ${response.code()}, error: $errorBody")
                return Pair("Failed to refresh token: ${response.message()}", null)
            }
        } catch (ex: IOException) {
            Log.e(TAG, "IOException: ${ex.message}", ex)
            return Pair("Network error: ${ex.message}", null)
        } catch (ex: Exception) {
            Log.e(TAG, "Exception: ${ex.message}", ex)
            return Pair("Error: ${ex.message}", null)
        }
    }
}

