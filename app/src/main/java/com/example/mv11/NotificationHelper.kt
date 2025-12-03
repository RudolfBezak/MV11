package com.example.mv11

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    private const val CHANNEL_ID = "kanal-1"
    private const val NOTIFICATION_ID = 1

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_name), importance).apply {
                description = context.getString(R.string.notification_channel_description)
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        text: String,
        notificationId: Int = NOTIFICATION_ID
    ) {
        createNotificationChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setContentTitle(title)
            setContentText(text)
            setSmallIcon(android.R.drawable.ic_dialog_info)
            priority = NotificationCompat.PRIORITY_DEFAULT
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("NotificationHelper", context.getString(R.string.notification_permission_error))
            return
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    fun showUserCountNotification(
        context: Context,
        title: String,
        message: String
    ) {
        createNotificationChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setContentTitle(title)
            setContentText(message)
            setSmallIcon(android.R.drawable.ic_dialog_info)
            priority = NotificationCompat.PRIORITY_DEFAULT
            setAutoCancel(true)
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("NotificationHelper", context.getString(R.string.notification_permission_error))
            return
        }

        // Použiť unikátne ID pre každú notifikáciu o používateľoch
        val notificationId = System.currentTimeMillis().toInt()
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }
}

