package com.example.mv11

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.mv11.databinding.FragmentMapBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.locationcomponent.locationComponentSettings
import kotlinx.coroutines.launch

/**
 * MapFragment - Fragment pre zobrazenie mapy s GPS polohou.
 * 
 * Funkcionalita:
 * - Zobrazenie vlastnej GPS polohy na mape
 * - Zobrazenie polôh ostatných používateľov
 * - Automatické odoslanie polohy na server pri zmene
 * - LocationComponent s pulsing efektom
 */
class MapFragment : Fragment() {

    private var binding: FragmentMapBinding? = null
    private lateinit var viewModel: UserFeedViewModel
    
    private var lastLocation: Point? = null
    private var annotationManager: com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager? = null

    /**
     * Pole povolení potrebných pre GPS.
     */
    private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    /**
     * ActivityResultLauncher pre požiadavku na GPS povolenie.
     */
    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MapFragment", "GPS permission granted")
            initLocationComponent()
            addLocationListeners()
        } else {
            Log.d("MapFragment", "GPS permission denied")
            Snackbar.make(
                binding?.root ?: return@registerForActivityResult,
                "GPS permission is required to show your location",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Kontroluje či má aplikácia všetky potrebné povolenia.
     */
    fun hasPermissions(context: Context): Boolean {
        return PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UserFeedViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[UserFeedViewModel::class.java]

        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
        }

        val hasPermission = hasPermissions(requireContext())
        onMapReady(hasPermission)

        // Nastavenie click listeneru pre FloatingActionButton
        binding?.myLocation?.setOnClickListener {
            if (!hasPermissions(requireContext())) {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                lastLocation?.let { refreshLocation(it) }
                addLocationListeners()
                Log.d("MapFragment", "location click")
            }
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

        // Načítaj geofence dáta pri otvorení fragmentu
        lifecycleScope.launch {
            viewModel.updateItems()
        }
    }

    /**
     * Inicializuje mapu a nastaví kameru.
     */
    private fun onMapReady(enabled: Boolean) {
        binding?.mapView?.getMapboxMap()?.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(17.1077, 48.1486)) // Bratislava
                .zoom(2.0)
                .build()
        )

        binding?.mapView?.getMapboxMap()?.loadStyleUri(Style.MAPBOX_STREETS) {
            if (enabled) {
                initLocationComponent()
                addLocationListeners()
            }
            setupUserMarkers()
        }

        // Listener pre kliknutie na mapu - zruší tracking
        binding?.mapView?.getMapboxMap()?.addOnMapClickListener {
            if (hasPermissions(requireContext())) {
                onCameraTrackingDismissed()
            }
            true
        }
    }

    /**
     * Inicializuje LocationComponent pre zobrazenie vlastnej polohy.
     */
    private fun initLocationComponent() {
        Log.d("MapFragment", "initLocationComponent")
        
        binding?.mapView?.location?.updateSettings {
            this.enabled = true
            this.pulsingEnabled = true
        }
    }

    /**
     * Pridá location listeners pre sledovanie zmien polohy.
     */
    private fun addLocationListeners() {
        Log.d("MapFragment", "addLocationListeners")
        
        binding?.mapView?.location?.addOnIndicatorPositionChangedListener(
            onIndicatorPositionChangedListener
        )
        binding?.mapView?.gestures?.addOnMoveListener(onMoveListener)
    }

    /**
     * Listener pre zmenu polohy indikátora (vlastná poloha používateľa).
     */
    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        Log.d("MapFragment", "poloha je $point")
        refreshLocation(point)
    }

    /**
     * Aktualizuje mapu na novú polohu a odošle ju na server.
     */
    private fun refreshLocation(point: Point) {
        binding?.mapView?.getMapboxMap()?.setCamera(
            CameraOptions.Builder()
                .center(point)
                .zoom(14.0)
                .build()
        )
        
        binding?.mapView?.gestures?.focalPoint = 
            binding?.mapView?.getMapboxMap()?.pixelForCoordinate(point)
        
        lastLocation = point
        addMarker(point)

        // Odošli polohu na server ak je sharing zapnutý
        val isSharing = PreferenceData.getInstance().getSharing(requireContext())
        if (isSharing) {
            lifecycleScope.launch {
                val error = viewModel.repository.apiUpdateGeofence(
                    point.latitude(),
                    point.longitude(),
                    100.0 // Default radius
                )
                if (error.isNotEmpty()) {
                    Log.e("MapFragment", "Failed to update geofence: $error")
                } else {
                    Log.d("MapFragment", "Geofence updated successfully")
                }
            }
        }
    }

    /**
     * Pridá marker na mapu na zadanú polohu.
     */
    private fun addMarker(point: Point) {
        binding?.mapView?.let { map ->
            if (annotationManager == null) {
                annotationManager = map.annotations.createPointAnnotationManager()
            }
            
            // Vymaž staré markery
            annotationManager?.deleteAll()
            
            // Pridaj nový marker
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(createMarkerBitmap("You"))
            
            annotationManager?.create(pointAnnotationOptions)
        }
    }

    /**
     * Listener pre pohybové gestá na mape.
     */
    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: com.mapbox.maps.plugin.gestures.MoveGestureDetector) {
            onCameraTrackingDismissed()
        }

        override fun onMove(detector: com.mapbox.maps.plugin.gestures.MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveEnd(detector: com.mapbox.maps.plugin.gestures.MoveGestureDetector) {}
    }

    /**
     * Zruší tracking polohy kamery.
     */
    private fun onCameraTrackingDismissed() {
        binding?.mapView?.apply {
            location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            gestures.removeOnMoveListener(onMoveListener)
        }
    }

    /**
     * Nastaví markery pre používateľov z databázy.
     */
    private fun setupUserMarkers() {
        viewModel.feed_items.observe(viewLifecycleOwner) { users ->
            val validUsers = users.filterNotNull()
            Log.d("MapFragment", "Zobrazujem ${validUsers.size} používateľov na mape")

            if (validUsers.isEmpty()) {
                Log.w("MapFragment", "Žiadni používatelia v databáze")
                return@observe
            }

            binding?.mapView?.let { map ->
                if (annotationManager == null) {
                    annotationManager = map.annotations.createPointAnnotationManager()
                }

                // Pridaj markery pre všetkých používateľov
                validUsers.forEach { user ->
                    Log.d("MapFragment", "Pridávam marker pre: ${user.name} na [${user.lat}, ${user.lon}]")

                    val pointAnnotationOptions = PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(user.lon, user.lat))
                        .withIconImage(createMarkerBitmap(user.name))

                    annotationManager?.create(pointAnnotationOptions)
                }

                // Ak máme používateľov, nastav kameru na prvého
                if (validUsers.isNotEmpty() && lastLocation == null) {
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

    /**
     * Vytvorí bitmap pre marker.
     */
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
        binding?.mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding?.mapView?.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        
        // Upratanie listenerov
        binding?.mapView?.apply {
            location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            gestures.removeOnMoveListener(onMoveListener)
        }
        
        binding?.mapView?.onDestroy()
        binding = null
    }
}
