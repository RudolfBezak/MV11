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
                    ApiService.create(context),  // ← Pridá Context pre interceptory
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
                    return Pair("", User(username, email, body.uid, body.access, body.refresh, ""))
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
            // Token a API key sa pridajú automaticky cez AuthInterceptor
            val response = service.listGeofence()
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

    fun getUsers() = cache.getUsers()

    /**
     * Získa údaje o používateľovi z API.
     * 
     * Token sa pridá automaticky cez AuthInterceptor.
     * Ak token expiroval (401), TokenAuthenticator ho automaticky obnoví.
     * 
     * @param uid - uid používateľa ktorého chceme získať
     * @return Pair<String, User?> - error message a User objekt (alebo null pri chybe)
     */
    suspend fun apiGetUser(uid: String): Pair<String, User?> {
        try {
            Log.d(TAG, "Fetching user with uid: $uid")
            
            // Token sa pridá automaticky cez AuthInterceptor
            // Ak expiroval, TokenAuthenticator ho automaticky obnoví
            val response = service.getUser(uid)

            Log.d(TAG, "GetUser response code: ${response.code()}")

            if (response.isSuccessful) {
                response.body()?.let { userResponse ->
                    Log.d(TAG, "User loaded successfully: ${userResponse.name}")
                    
                    // Získaj aktuálne tokeny z SharedPreferences (môžu byť obnovené)
                    val currentUser = PreferenceData.getInstance().getUser(null)
                    
                    return Pair(
                        "",
                        User(
                            userResponse.name,
                            currentUser?.email ?: "",  // Email z SharedPreferences
                            userResponse.uid,
                            currentUser?.access ?: "",  // Aktuálny access token (môže byť obnovený)
                            currentUser?.refresh ?: "",  // Aktuálny refresh token
                            userResponse.photo
                        )
                    )
                }
            }

            Log.e(TAG, "Failed to load user: ${response.message()} (code: ${response.code()})")
            return Pair("Failed to load user", null)
            
        } catch (ex: IOException) {
            Log.e(TAG, "IOException: ${ex.message}", ex)
            ex.printStackTrace()
            return Pair("Check internet connection. Failed to load user.", null)
        } catch (ex: Exception) {
            Log.e(TAG, "Exception: ${ex.message}", ex)
            ex.printStackTrace()
        }
        return Pair("Fatal error. Failed to load user.", null)
    }

    /**
     * Odosle aktuálnu polohu používateľa na server.
     * 
     * @param lat - zemepisná šírka (latitude)
     * @param lon - zemepisná dĺžka (longitude)
     * @param radius - polomer geofence oblasti v metroch
     * @return String - error message alebo prázdny string pri úspechu
     */
    suspend fun apiUpdateGeofence(lat: Double, lon: Double, radius: Double): String {
        try {
            Log.d(TAG, "Updating geofence: lat=$lat, lon=$lon, radius=$radius")
            
            val response = service.updateGeofence(
                GeofenceUpdateRequest(lat, lon, radius)
            )
            
            Log.d(TAG, "UpdateGeofence response code: ${response.code()}")
            
            if (response.isSuccessful) {
                Log.d(TAG, "Geofence updated successfully")
                return ""
            }
            
            Log.e(TAG, "Failed to update geofence: ${response.message()} (code: ${response.code()})")
            return "Failed to update location"
            
        } catch (ex: IOException) {
            Log.e(TAG, "IOException: ${ex.message}", ex)
            ex.printStackTrace()
            return "Check internet connection. Failed to update location."
        } catch (ex: Exception) {
            Log.e(TAG, "Exception: ${ex.message}", ex)
            ex.printStackTrace()
        }
        return "Fatal error. Failed to update location."
    }

    /**
     * Odstráni polohu používateľa zo servera.
     * 
     * @return String - error message alebo prázdny string pri úspechu
     */
    suspend fun apiDeleteGeofence(): String {
        try {
            Log.d(TAG, "Deleting geofence")
            
            val response = service.deleteGeofence()
            
            Log.d(TAG, "DeleteGeofence response code: ${response.code()}")
            
            if (response.isSuccessful) {
                Log.d(TAG, "Geofence deleted successfully")
                return ""
            }
            
            Log.e(TAG, "Failed to delete geofence: ${response.message()} (code: ${response.code()})")
            return "Failed to delete location"
            
        } catch (ex: IOException) {
            Log.e(TAG, "IOException: ${ex.message}", ex)
            ex.printStackTrace()
            return "Check internet connection. Failed to delete location."
        } catch (ex: Exception) {
            Log.e(TAG, "Exception: ${ex.message}", ex)
            ex.printStackTrace()
        }
        return "Fatal error. Failed to delete location."
    }

    /**
     * Získa zoznam všetkých polôh používateľov (vrátane vlastnej).
     * 
     * @return Pair<String, List<GeofenceResponse>?> - error message a zoznam polôh (alebo null pri chybe)
     */
    suspend fun apiListGeofenceLocations(): Pair<String, List<GeofenceResponse>?> {
        try {
            Log.d(TAG, "Fetching geofence locations from API")
            
            val response = service.listGeofence()
            
            Log.d(TAG, "ListGeofence response code: ${response.code()}")
            
            if (response.isSuccessful) {
                response.body()?.let { locations ->
                    Log.d(TAG, "Received ${locations.size} geofence locations from API")
                    
                    // Konvertuj GeofenceResponse na UserEntity a ulož do databázy
                    val users = locations.map {
                        UserEntity(
                            it.uid, it.name, it.updated,
                            it.lat, it.lon, it.radius, ""
                        )
                    }
                    cache.insertUserItems(users)
                    Log.d(TAG, "Geofence locations saved to database")
                    
                    return Pair("", locations)
                }
            }
            
            Log.e(TAG, "Failed to load geofence locations: ${response.message()} (code: ${response.code()})")
            return Pair("Failed to load locations", null)
            
        } catch (ex: IOException) {
            Log.e(TAG, "IOException: ${ex.message}", ex)
            ex.printStackTrace()
            return Pair("Check internet connection. Failed to load locations.", null)
        } catch (ex: Exception) {
            Log.e(TAG, "Exception: ${ex.message}", ex)
            ex.printStackTrace()
        }
        return Pair("Fatal error. Failed to load locations.", null)
    }
}

