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
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager

class MapFragment : Fragment() {
    
    private var mapView: MapView? = null
    private lateinit var viewModel: UserFeedViewModel
    
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
        viewModel.feed_items.observe(viewLifecycleOwner) { users ->
            val validUsers = users.filterNotNull()
            Log.d("MapFragment", "Zobrazujem ${validUsers.size} používateľov na mape")
            
            if (validUsers.isEmpty()) {
                Log.w("MapFragment", "Žiadni používatelia v databáze")
                return@observe
            }
            
            mapView?.let { map ->
                val annotationApi = map.annotations
                val pointAnnotationManager = annotationApi.createPointAnnotationManager()
                
                validUsers.forEach { user ->
                    Log.d("MapFragment", "Pridávam marker pre: ${user.name} na [${user.lat}, ${user.lon}]")
                    
                    val pointAnnotationOptions = PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(user.lon, user.lat))
                        .withIconImage(createMarkerBitmap(user.name))
                    
                    pointAnnotationManager.create(pointAnnotationOptions)
                }
                
                if (validUsers.isNotEmpty()) {
                    val firstUser = validUsers[0]
                    map.getMapboxMap().setCamera(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(firstUser.lon, firstUser.lat))
                            .zoom(10.0)
                            .build()
                    )
                }
            }
        }
    }
    
    private fun createMarkerBitmap(name: String): Bitmap {
        val size = 100
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        
        paint.color = Color.WHITE
        paint.textSize = 30f
        paint.textAlign = Paint.Align.CENTER
        
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

