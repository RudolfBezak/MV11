package com.example.mv11

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import android.app.PendingIntent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) {
            Log.e("GeofenceBroadcastReceiver", "error 1")
            return
        }

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.e("GeofenceBroadcastReceiver", "error 2")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e("GeofenceBroadcastReceiver", "error 3: $errorMessage")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            val triggeringLocation = geofencingEvent.triggeringLocation
            if (context == null || triggeringLocation == null) {
                Log.e("GeofenceBroadcastReceiver", "error 4")
                return
            }
            
            Log.d("GeofenceBroadcastReceiver", "User exited geofence at [${triggeringLocation.latitude}, ${triggeringLocation.longitude}]")
            refreshGeofence(triggeringLocation, context)
        }
    }

    private fun refreshGeofence(location: Location, context: Context) {
        val user = PreferenceData.getInstance().getUser(context)
        val currentLocation = PreferenceData.getInstance().getCurrentLocation(context)
        val radius = currentLocation?.third ?: 100.0
        
        if (user == null || user.access.isEmpty()) {
            Log.e("GeofenceBroadcastReceiver", "User not logged in, cannot refresh geofence")
            setupGeofence(location, context, radius)
            return
        }

        Log.d("GeofenceBroadcastReceiver", "Refreshing geofence on server: lat=${location.latitude}, lon=${location.longitude}, radius=$radius")
        
        scope.launch(Dispatchers.IO) {
            val repository = DataRepository.getInstance(context)
            val result = repository.apiUpdateGeofence(user.access, location.latitude, location.longitude, radius)
            
            if (result.second) {
                Log.d("GeofenceBroadcastReceiver", "Geofence refreshed successfully on server")
                PreferenceData.getInstance().setCurrentLocation(context, location.latitude, location.longitude, radius)
                
                scope.launch(Dispatchers.Main) {
                    setupGeofence(location, context, radius)
                }
            } else {
                Log.e("GeofenceBroadcastReceiver", "Failed to refresh geofence on server: ${result.first}")
                scope.launch(Dispatchers.Main) {
                    setupGeofence(location, context, radius)
                }
            }
        }
    }

    private fun setupGeofence(location: Location, context: Context, radius: Double) {
        val geofencingClient = LocationServices.getGeofencingClient(context.applicationContext)
        val radiusFloat = radius.toFloat()

        val geofence = Geofence.Builder()
            .setRequestId("my-geofence")
            .setCircularRegion(location.latitude, location.longitude, radiusFloat)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val geofencePendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PERMISSION_GRANTED
        ) {
            Log.e("GeofenceBroadcastReceiver", "error 5: permission denied")
            return
        }

        geofencingClient.removeGeofences(geofencePendingIntent).addOnCompleteListener {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    Log.d("GeofenceBroadcastReceiver", "Nový geofence vytvorený na pozícii [${location.latitude}, ${location.longitude}] s polomerom $radius m")
                }
                addOnFailureListener {
                    Log.e("GeofenceBroadcastReceiver", "error 6: ${it.message}")
                }
            }
        }
    }
}

