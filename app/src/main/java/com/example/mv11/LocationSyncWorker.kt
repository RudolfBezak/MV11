package com.example.mv11

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class LocationSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

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

            val isLocationSharingEnabled = PreferenceData.getInstance()
                .getLocationSharingEnabled(applicationContext)

            if (!isLocationSharingEnabled) {
                Log.d(TAG, "Location sharing is disabled, skipping sync")
                return Result.success()
            }

            val repository = DataRepository.getInstance(applicationContext)
            val result = repository.apiListGeofence(accessToken)

            if (result.second) {
                // result.third contains GeofenceMe object, but we don't need it in Worker
                Log.d(TAG, "Location sync successful")
                NotificationHelper.showNotification(
                    applicationContext,
                    applicationContext.getString(R.string.location_sync_completed),
                    applicationContext.getString(R.string.location_sync_success)
                )
                Result.success()
            } else {
                Log.e(TAG, "Location sync failed: ${result.first}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during location sync", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "LocationSyncWorker"
        const val KEY_ACCESS_TOKEN = "access_token"

        fun createInputData(accessToken: String) = workDataOf(KEY_ACCESS_TOKEN to accessToken)
    }
}

