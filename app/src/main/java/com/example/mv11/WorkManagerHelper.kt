package com.example.mv11

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkManagerHelper {
    private const val UNIQUE_SYNC_WORK_NAME = "location_sync_work"
    private const val UNIQUE_UPDATE_WORK_NAME = "location_update_work"

    fun startPeriodicLocationSync(context: Context, accessToken: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<LocationSyncWorker>(
            1, TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInputData(LocationSyncWorker.createInputData(accessToken))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun stopPeriodicLocationSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_SYNC_WORK_NAME)
    }

    fun startAutoLocationUpdate(context: Context, accessToken: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<LocationUpdateWorker>(
            30, TimeUnit.MINUTES,
            15, TimeUnit.MINUTES  // Flex interval - minimum je 15 minút, práca sa môže spustiť v posledných 15 minútach z 30-minútového intervalu
        )
            .setConstraints(constraints)
            .setInputData(LocationUpdateWorker.createInputData(accessToken))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_UPDATE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun stopAutoLocationUpdate(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_UPDATE_WORK_NAME)
    }
}

