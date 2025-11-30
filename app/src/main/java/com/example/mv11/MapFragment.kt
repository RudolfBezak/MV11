package com.example.mv11

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager

class MapFragment : Fragment() {
    
    private var mapView: MapView? = null
    private lateinit var viewModel: UserFeedViewModel
    private var pointAnnotationManager: com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager? = null
    
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
                    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(view, "Používatelia načítaní!", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun setupUserMarkers() {
        mapView?.let { map ->
            val annotationApi = map.annotations
            pointAnnotationManager = annotationApi.createPointAnnotationManager()
        }
        
        // Observe current user location from SharedPreferences
        val currentLocation = PreferenceData.getInstance().getCurrentLocation(requireContext())
        if (currentLocation != null && currentLocation.first != 0.0 && currentLocation.second != 0.0) {
            addCurrentUserMarker(currentLocation.first, currentLocation.second, currentLocation.third)
        }
        
        // Also observe changes in location
        viewModel.feed_items.observe(viewLifecycleOwner) { users ->
            val currentLocation = PreferenceData.getInstance().getCurrentLocation(requireContext())
            if (currentLocation != null && currentLocation.first != 0.0 && currentLocation.second != 0.0) {
                addCurrentUserMarker(currentLocation.first, currentLocation.second, currentLocation.third)
            }
        }
    }
    
    private fun addCurrentUserMarker(lat: Double, lon: Double, radius: Double) {
        mapView?.let { map ->
            // Clear existing markers
            pointAnnotationManager?.deleteAll()
            
            // Get current user name
            val currentUser = PreferenceData.getInstance().getUser(requireContext())
            val userName = currentUser?.name ?: "Ja"
            
            Log.d("MapFragment", "Pridávam marker pre aktuálneho používateľa: $userName na [$lat, $lon] s radiusom $radius m")
            
            // Add marker for current user
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(lon, lat))
                .withIconImage(createMarkerBitmap(userName))
            
            pointAnnotationManager?.create(pointAnnotationOptions)
            
            // Add circle around current user
            addCircleAroundUser(lat, lon, radius)
            
            // Center camera on current user
            map.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(lon, lat))
                    .zoom(calculateZoomForRadius(radius))
                    .build()
            )
        }
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
            
            // Add fill layer for circle using extension function
            style.addLayer(
                fillLayer(layerId, sourceId) {
                    fillColor("#FFD500") // Yellow color
                    fillOpacity(0.3)
                }
            )
            
            // Add stroke layer for circle border using extension function
            style.addLayer(
                lineLayer(strokeLayerId, sourceId) {
                    lineColor("#FFD500") // Yellow color
                    lineWidth(2.0)
                }
            )
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
    
    private fun createMarkerBitmap(name: String): Bitmap {
        val size = 100
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint().apply {
            color = Color.parseColor("#FFD500") // Yellow color (secondary_yellow)
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

