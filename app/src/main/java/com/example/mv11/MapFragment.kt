package com.example.mv11

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

class MapFragment : Fragment() {
    
    private var mapView: MapView? = null
    private lateinit var viewModel: UserFeedViewModel
    private var pointAnnotationManager: com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager? = null
    private val markerUserIdMap = mutableMapOf<Long, String>() // Map annotation ID to userId
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UserFeedViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[UserFeedViewModel::class.java]
        
        mapView = view.findViewById(R.id.mapView)
        mapView?.getMapboxMap()?.loadStyleUri(Style.MAPBOX_STREETS) {
            setupUserMarkers()
        }
        
        val bottomNav = view.findViewById<BottomNavigationWidget>(R.id.bottomNavigationWidget)
        bottomNav.setActiveItem(BottomNavItem.MAP)
        
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("MapFragment", "Loading: $isLoading")
        }
        
        viewModel.message.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { message ->
                if (message.isNotEmpty()) {
                    if (message == "MUSIS_SI_ZAPNUT_GEOFENCE") {
                        Snackbar.make(view, "Musíš si zapnúť geofence na videnie používateľov", Snackbar.LENGTH_LONG).show()
                    } else {
                        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        // Load users on fragment creation
        val user = PreferenceData.getInstance().getUser(requireContext())
        if (user != null && user.access.isNotEmpty()) {
            viewModel.updateItems(user.access)
        }
    }
    
    private fun setupUserMarkers() {
        val mapViewInstance = mapView
        if (mapViewInstance != null) {
            val annotationApi = mapViewInstance.annotations
            pointAnnotationManager = annotationApi.createPointAnnotationManager()
            
            // Add click listener for markers
            pointAnnotationManager?.addClickListener { annotation: PointAnnotation ->
                val annotationId: Long = annotation.id
                val userId: String? = markerUserIdMap[annotationId]
                if (userId != null && userId.isNotEmpty()) {
                    val bundle = Bundle().apply {
                        putString("userId", userId)
                    }
                    findNavController().navigate(R.id.profileFragment, bundle)
                }
                true
            }
        }
        
        // Observe current user location from SharedPreferences
        val currentLocation = PreferenceData.getInstance().getCurrentLocation(requireContext())
        if (currentLocation != null && currentLocation.first != 0.0 && currentLocation.second != 0.0) {
            val userList = viewModel.feed_items.value?.filterNotNull() ?: emptyList()
            addCurrentUserMarker(currentLocation.first, currentLocation.second, currentLocation.third, userList)
        }
        
        // Also observe changes in location and users list
        viewModel.feed_items.observe(viewLifecycleOwner) { users ->
            val currentLocation = PreferenceData.getInstance().getCurrentLocation(requireContext())
            if (currentLocation != null && currentLocation.first != 0.0 && currentLocation.second != 0.0) {
                val userList = users.filterNotNull()
                addCurrentUserMarker(currentLocation.first, currentLocation.second, currentLocation.third, userList)
            }
        }
    }
    
    private fun addCurrentUserMarker(lat: Double, lon: Double, radius: Double, users: List<UserEntity>) {
        mapView?.let { map ->
            // Clear existing markers and map
            pointAnnotationManager?.deleteAll()
            markerUserIdMap.clear()
            
            // Get current user name
            val currentUser = PreferenceData.getInstance().getUser(requireContext())
            val userName = currentUser?.name ?: "Ja"
            
            Log.d("MapFragment", "Pridávam marker pre aktuálneho používateľa: $userName na [$lat, $lon] s radiusom $radius m")
            
            // Add circle FIRST with low z-order (below markers)
            addCircleAroundUser(lat, lon, radius)
            
            // Add marker for current user (red border, thicker)
            val currentUserPhoto = currentUser?.let { getUserPhotoFromList(it.uid, users) } ?: ""
            val currentUserId = currentUser?.uid ?: ""
            createMarkerWithPhoto(userName, currentUserPhoto, isCurrentUser = true) { bitmap ->
                val pointAnnotationOptions = PointAnnotationOptions()
                    .withPoint(Point.fromLngLat(lon, lat))
                    .withIconImage(bitmap)
                val annotation = pointAnnotationManager?.create(pointAnnotationOptions)
                if (annotation != null && currentUserId.isNotEmpty()) {
                    markerUserIdMap[annotation.id] = currentUserId
                }
            }
            
            // Add markers for other users (without lat/lon) - random positions in circle
            val usersWithoutLocation = users.filter { it.lat == 0.0 && it.lon == 0.0 }
            usersWithoutLocation.forEach { user ->
                val randomPosition = generateRandomPositionInCircle(lat, lon, radius)
                createMarkerWithPhoto(user.name, user.photo, isCurrentUser = false) { bitmap ->
                    val userMarkerOptions = PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(randomPosition.second, randomPosition.first))
                        .withIconImage(bitmap)
                    val annotation = pointAnnotationManager?.create(userMarkerOptions)
                    if (annotation != null) {
                        markerUserIdMap[annotation.id] = user.uid
                    }
                    Log.d("MapFragment", "Pridávam marker pre používateľa: ${user.name} na náhodnej pozícii [${randomPosition.first}, ${randomPosition.second}]")
                }
            }
            
            // Center camera on current user
            map.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(lon, lat))
                    .zoom(calculateZoomForRadius(radius))
                    .build()
            )
        }
    }
    
    private fun generateRandomPositionInCircle(centerLat: Double, centerLon: Double, radiusMeters: Double): Pair<Double, Double> {
        // Generate random angle (0 to 2π)
        val randomAngle = Math.random() * 2 * Math.PI
        
        // Generate random distance (0 to radius) - using square root for uniform distribution
        val randomDistance = Math.sqrt(Math.random()) * radiusMeters
        
        // Convert distance from meters to degrees
        val latRad = Math.toRadians(centerLat)
        val metersPerDegreeLat = 111320.0
        val metersPerDegreeLon = 111320.0 * Math.cos(latRad)
        
        val distanceLat = randomDistance / metersPerDegreeLat
        val distanceLon = randomDistance / metersPerDegreeLon
        
        // Calculate new position
        val newLat = centerLat + distanceLat * Math.cos(randomAngle)
        val newLon = centerLon + distanceLon * Math.sin(randomAngle)
        
        return Pair(newLat, newLon)
    }
    
    private fun addCircleAroundUser(lat: Double, lon: Double, radiusMeters: Double) {
        mapView?.getMapboxMap()?.getStyle()?.let { style ->
            val centerPoint = Point.fromLngLat(lon, lat)
            
            // Create circle polygon coordinates
            val circleCoordinates = createCircleCoordinates(centerPoint, radiusMeters)
            val circlePolygon = com.mapbox.geojson.Polygon.fromLngLats(listOf(circleCoordinates))
            val circleFeature = Feature.fromGeometry(circlePolygon)
            val featureCollection = FeatureCollection.fromFeature(circleFeature)
            
            val sourceId = "circle-source"
            val layerId = "circle-layer"
            val strokeLayerId = "${layerId}-stroke"
            
            // Remove existing source and layers if they exist
            try {
                style.removeStyleLayer(strokeLayerId)
            } catch (e: Exception) {
                // Ignore if doesn't exist
            }
            try {
                style.removeStyleLayer(layerId)
            } catch (e: Exception) {
                // Ignore if doesn't exist
            }
            try {
                style.removeStyleSource(sourceId)
            } catch (e: Exception) {
                // Ignore if doesn't exist
            }
            
            // Add circle source using extension function
            style.addSource(
                geoJsonSource(sourceId) {
                    featureCollection(featureCollection)
                }
            )
            
            // Add fill layer for circle BELOW other layers (so markers are on top)
            // Try to add below "water" layer, if it doesn't exist, add normally
            try {
                style.addLayerBelow(
                    fillLayer(layerId, sourceId) {
                        fillColor("#FFD500") // Yellow color
                        fillOpacity(0.1)
                    },
                    "water" // Add below water layer
                )
            } catch (e: Exception) {
                // If "water" layer doesn't exist, add normally (will be below annotations)
                style.addLayer(
                    fillLayer(layerId, sourceId) {
                        fillColor("#FFD500") // Yellow color
                        fillOpacity(0.1)
                    }
                )
            }
            
            // Add stroke layer for circle border BELOW other layers
            try {
                style.addLayerBelow(
                    lineLayer(strokeLayerId, sourceId) {
                        lineColor("#FFD500") // Yellow color
                        lineWidth(2.0)
                    },
                    "water" // Add below water layer
                )
            } catch (e: Exception) {
                // If "water" layer doesn't exist, add normally (will be below annotations)
                style.addLayer(
                    lineLayer(strokeLayerId, sourceId) {
                        lineColor("#FFD500") // Yellow color
                        lineWidth(2.0)
                    }
                )
            }
        }
    }
    
    private fun createCircleCoordinates(center: Point, radiusMeters: Double): List<Point> {
        val points = mutableListOf<Point>()
        val numPoints = 64 // Number of points to create smooth circle
        
        // Convert radius from meters to degrees
        // At equator: 1 degree ≈ 111,320 meters
        // Adjust for latitude
        val latRad = Math.toRadians(center.latitude())
        val metersPerDegreeLat = 111320.0
        val metersPerDegreeLon = 111320.0 * Math.cos(latRad)
        
        val radiusLat = radiusMeters / metersPerDegreeLat
        val radiusLon = radiusMeters / metersPerDegreeLon
        
        for (i in 0 until numPoints) {
            val angle = 2 * Math.PI * i / numPoints
            val lat = center.latitude() + radiusLat * Math.cos(angle)
            val lon = center.longitude() + radiusLon * Math.sin(angle)
            points.add(Point.fromLngLat(lon, lat))
        }
        
        // Close the circle
        points.add(points[0])
        
        return points
    }
    
    private fun calculateZoomForRadius(radiusMeters: Double): Double {
        // Approximate zoom level calculation
        // Larger radius = lower zoom (zoom out)
        return when {
            radiusMeters >= 5000 -> 11.0
            radiusMeters >= 1000 -> 12.0
            radiusMeters >= 500 -> 13.0
            radiusMeters >= 200 -> 14.0
            else -> 15.0
        }
    }
    
    private fun getUserPhotoFromList(uid: String, users: List<UserEntity>): String {
        return users.find { it.uid == uid }?.photo ?: ""
    }
    
    private fun createMarkerWithPhoto(name: String, photoPath: String, isCurrentUser: Boolean = false, callback: (Bitmap) -> Unit) {
        val size = 100
        
        if (photoPath.isNotEmpty()) {
            // Clean photo path - remove "../" if present
            val cleanPhotoPath = photoPath.replace("../", "")
            // Build full URL with prefix
            val photoUrl = "https://upload.mcomputing.eu/$cleanPhotoPath"
            
            // Create placeholder bitmap
            val placeholderBitmap = createMarkerBitmap(name, isCurrentUser)
            val placeholderDrawable = BitmapDrawable(requireContext().resources, placeholderBitmap)
            
            // Load photo asynchronously using Glide
            Glide.with(requireContext())
                .asBitmap()
                .load(photoUrl)
                .placeholder(placeholderDrawable)
                .error(placeholderBitmap)
                .circleCrop()
                .into(object : CustomTarget<Bitmap>(size, size) {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        // Create circular bitmap with photo
                        val circularBitmap = createCircularBitmap(resource, size, isCurrentUser)
                        callback(circularBitmap)
                    }
                    
                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                        // Use placeholder if load is cleared
                        callback(placeholderBitmap)
                    }
                })
        } else {
            // No photo available, use text marker
            callback(createMarkerBitmap(name, isCurrentUser))
        }
    }
    
    private fun createCircularBitmap(source: Bitmap, size: Int, isCurrentUser: Boolean = false): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val paint = Paint().apply {
            isAntiAlias = true
        }
        
        // Scale source bitmap to fit
        val scaledBitmap = Bitmap.createScaledBitmap(source, size, size, true)
        
        val radius = size / 2f
        
        // Draw circular mask first
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawCircle(radius, radius, radius, paint)
        
        // Use PorterDuff mode to clip bitmap to circle
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)
        
        // Draw border - red and thicker for current user, yellow for others
        paint.xfermode = null
        paint.style = Paint.Style.STROKE
        if (isCurrentUser) {
            paint.color = Color.RED // Red border for current user
            paint.strokeWidth = 8f // Thicker border
        } else {
            paint.color = Color.parseColor("#FFD500") // Yellow border
            paint.strokeWidth = 4f
        }
        val borderOffset = if (isCurrentUser) 4f else 2f
        canvas.drawCircle(radius, radius, radius - borderOffset, paint)
        
        return output
    }
    
    private fun createMarkerBitmap(name: String, isCurrentUser: Boolean = false): Bitmap {
        val size = 100
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint().apply {
            // Red for current user, yellow for others
            color = if (isCurrentUser) Color.RED else Color.parseColor("#FFD500")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        
        paint.color = Color.BLACK
        paint.textSize = 30f
        paint.textAlign = Paint.Align.CENTER
        paint.style = Paint.Style.FILL
        
        val initial = if (name.isNotEmpty()) name[0].toString().uppercase() else "?"
        canvas.drawText(initial, size / 2f, size / 2f + 10f, paint)
        
        return bitmap
    }
    
    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }
    
    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDestroy()
        mapView = null
    }
}

