package com.example.mv11

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

class LocationUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)

    override suspend fun doWork(): Result {
        return try {
            val accessToken = inputData.getString(KEY_ACCESS_TOKEN) ?: ""

            if (accessToken.isEmpty()) {
                Log.e(TAG, "Access token is empty")
                return Result.failure()
            }

            val user = PreferenceData.getInstance().getUser(applicationContext)
            if (user == null) {
                Log.e(TAG, "User is not logged in")
                return Result.failure()
            }

            val isAutoLocationUpdateEnabled = PreferenceData.getInstance()
                .getAutoLocationUpdateEnabled(applicationContext)

            if (!isAutoLocationUpdateEnabled) {
                Log.d(TAG, "Auto location update is disabled, skipping update")
                return Result.success()
            }

            val isLocationSharingEnabled = PreferenceData.getInstance()
                .getLocationSharingEnabled(applicationContext)

            if (!isLocationSharingEnabled) {
                Log.d(TAG, "Location sharing is disabled, skipping update")
                return Result.success()
            }

            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "Location permissions not granted")
                return Result.failure()
            }

            val location: Location? = fusedLocationClient.lastLocation.await()

            if (location != null) {
                val repository = DataRepository.getInstance(applicationContext)
                val currentLocation = PreferenceData.getInstance().getCurrentLocation(applicationContext)
                val radius = currentLocation?.third ?: 100.0

                val result = repository.apiUpdateGeofence(
                    accessToken,
                    location.latitude,
                    location.longitude,
                    radius
                )

                if (result.second) {
                    Log.d(TAG, "Location updated successfully: lat=${location.latitude}, lon=${location.longitude}")
                    PreferenceData.getInstance().setCurrentLocation(
                        applicationContext,
                        location.latitude,
                        location.longitude,
                        radius
                    )
                    
                    // Po úspešnej aktualizácii geofence zavolať users list
                    val lastUserCount = PreferenceData.getInstance().getLastUserCount(applicationContext)
                    val listResult = repository.apiListGeofence(accessToken)
                    if (listResult.second) {
                        // Získať aktuálny počet používateľov z databázy (synchronne)
                        val users = repository.getUsersSync().filterNotNull()
                        val currentUserCount = users.size
                        
                        if (lastUserCount >= 0 && currentUserCount != lastUserCount) {
                            val difference = currentUserCount - lastUserCount
                            val message = when {
                                difference > 0 -> "Pribudlo $difference ${if (difference == 1) "používateľ" else if (difference < 5) "používatelia" else "používateľov"}. Celkom okolo vás: $currentUserCount"
                                difference < 0 -> "Ubudlo ${-difference} ${if (-difference == 1) "používateľ" else if (-difference < 5) "používatelia" else "používateľov"}. Celkom okolo vás: $currentUserCount"
                                else -> "Okolo vás je $currentUserCount ${if (currentUserCount == 1) "používateľ" else if (currentUserCount < 5) "používatelia" else "používateľov"}"
                            }
                            NotificationHelper.showUserCountNotification(
                                applicationContext,
                                "Zmena počtu používateľov",
                                message
                            )
                        } else if (lastUserCount < 0) {
                            // Prvé načítanie - len uložiť počet
                            NotificationHelper.showUserCountNotification(
                                applicationContext,
                                "Používatelia okolo vás",
                                "Okolo vás je $currentUserCount ${if (currentUserCount == 1) "používateľ" else if (currentUserCount < 5) "používatelia" else "používateľov"}"
                            )
                        }
                        
                        PreferenceData.getInstance().setLastUserCount(applicationContext, currentUserCount)
                    }
                    
                    Result.success()
                } else {
                    Log.e(TAG, "Location update failed: ${result.first}")
                    Result.retry()
                }
            } else {
                Log.e(TAG, "Failed to get current location")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during location update", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "LocationUpdateWorker"
        const val KEY_ACCESS_TOKEN = "access_token"

        fun createInputData(accessToken: String) = workDataOf(KEY_ACCESS_TOKEN to accessToken)
    }
}

