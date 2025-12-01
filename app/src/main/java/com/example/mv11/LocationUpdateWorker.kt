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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

            val timeFrom = PreferenceData.getInstance().getLocationSharingTimeFrom(applicationContext)
            val timeTo = PreferenceData.getInstance().getLocationSharingTimeTo(applicationContext)
            
            if (timeFrom.isNotEmpty() && timeTo.isNotEmpty()) {
                if (!isCurrentTimeInInterval(timeFrom, timeTo)) {
                    Log.d(TAG, "Current time is outside the specified interval ($timeFrom - $timeTo), skipping update")
                    return Result.success()
                }
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

    private fun isCurrentTimeInInterval(timeFrom: String, timeTo: String): Boolean {
        try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val fromTime = timeFormat.parse(timeFrom)
            val toTime = timeFormat.parse(timeTo)
            
            if (fromTime == null || toTime == null) {
                Log.e(TAG, "Failed to parse time interval: $timeFrom - $timeTo")
                return true
            }
            
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val currentTimeInMinutes = currentHour * 60 + currentMinute
            
            val fromCalendar = Calendar.getInstance().apply {
                time = fromTime
            }
            val fromHour = fromCalendar.get(Calendar.HOUR_OF_DAY)
            val fromMinute = fromCalendar.get(Calendar.MINUTE)
            val fromTimeInMinutes = fromHour * 60 + fromMinute
            
            val toCalendar = Calendar.getInstance().apply {
                time = toTime
            }
            val toHour = toCalendar.get(Calendar.HOUR_OF_DAY)
            val toMinute = toCalendar.get(Calendar.MINUTE)
            val toTimeInMinutes = toHour * 60 + toMinute
            
            // Ak je čas "od" väčší ako čas "do", znamená to interval cez polnoc
            // Napr. od 17:00 do 8:00 = od 17:00 do 24:00 + od 0:00 do 8:00
            if (fromTimeInMinutes > toTimeInMinutes) {
                // Interval cez polnoc: aktívny ak je čas >= fromTime alebo <= toTime
                return currentTimeInMinutes >= fromTimeInMinutes || currentTimeInMinutes <= toTimeInMinutes
            } else {
                // Normálny interval v rámci jedného dňa: aktívny ak je čas medzi fromTime a toTime
                return currentTimeInMinutes >= fromTimeInMinutes && currentTimeInMinutes <= toTimeInMinutes
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking time interval", e)
            return true
        }
    }

    companion object {
        private const val TAG = "LocationUpdateWorker"
        const val KEY_ACCESS_TOKEN = "access_token"

        fun createInputData(accessToken: String) = workDataOf(KEY_ACCESS_TOKEN to accessToken)
    }
}

