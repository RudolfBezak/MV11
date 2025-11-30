package com.example.mv11

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.content.res.ColorStateList
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController

class ProfileFragment : Fragment() {

    private lateinit var viewModel: AuthViewModel
    private var view: View? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val DEFAULT_RADIUS = 100.0 // meters

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val inflatedView = inflater.inflate(R.layout.fragment_profile, container, false)
        view = inflatedView
        return inflatedView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        viewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[AuthViewModel::class.java]

        updateUI()

        viewModel.logoutResult.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { result ->
                val currentView = view ?: return@observe
                if (result.second) {
                    Log.d("ProfileFragment", "Logout successful")
                    PreferenceData.getInstance().clearData(context)
                    updateUI()
                    Snackbar.make(
                        currentView.findViewById(R.id.btnLogout),
                        getString(R.string.logout_success),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    findNavController().navigate(R.id.prihlasenieFragment)
                } else {
                    Log.e("ProfileFragment", "Logout failed: ${result.first}")
                    Snackbar.make(
                        currentView.findViewById(R.id.btnLogout),
                        "${getString(R.string.logout_error)}: ${result.first}",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }

        viewModel.geofenceUpdateResult.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { result ->
                val currentView = view ?: return@observe
                if (result.second) {
                    Log.d("ProfileFragment", "Geofence updated successfully")
                    val currentLocation = PreferenceData.getInstance().getCurrentLocation(context)
                    if (currentLocation != null) {
                        updateUI() // Refresh UI to show updated radius
                    }
                    val targetView = currentView.findViewById<View>(R.id.btnUpdateRange) 
                        ?: currentView.findViewById(R.id.switchLocationSharing)
                    Snackbar.make(
                        targetView,
                        getString(R.string.radius_updated),
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    Log.e("ProfileFragment", "Geofence update failed: ${result.first}")
                    val switchLocationSharing = currentView.findViewById<SwitchMaterial>(R.id.switchLocationSharing)
                    switchLocationSharing.isChecked = false
                    PreferenceData.getInstance().setLocationSharingEnabled(context, false)
                    PreferenceData.getInstance().clearCurrentLocation(context)
                    updateUI() // Refresh UI
                    Snackbar.make(
                        switchLocationSharing,
                        result.first,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        viewModel.geofenceDeleteResult.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { result ->
                val currentView = view ?: return@observe
                if (result.second) {
                    Log.d("ProfileFragment", "Geofence deleted successfully")
                    PreferenceData.getInstance().clearCurrentLocation(context)
                    updateUI() // Refresh UI to hide coordinates
                    Snackbar.make(
                        currentView.findViewById(R.id.switchLocationSharing),
                        getString(R.string.location_sharing_disabled),
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    Log.e("ProfileFragment", "Geofence delete failed: ${result.first}")
                    Snackbar.make(
                        currentView.findViewById(R.id.switchLocationSharing),
                        result.first,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        setupClickListeners()
        setupLocationSharingToggle()
        setupChangePasswordClick()
        setupRadiusSlider()
        setupUpdateRangeButton()

        val bottomNav = view.findViewById<BottomNavigationWidget>(R.id.bottomNavigationWidget)
        bottomNav.setActiveItem(BottomNavItem.PROFILE)
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val currentView = view ?: return
        val user = PreferenceData.getInstance().getUser(context)
        val labelName = currentView.findViewById<TextView>(R.id.labelName)
        val tvUserName = currentView.findViewById<TextView>(R.id.tvUserName)
        val labelEmail = currentView.findViewById<TextView>(R.id.labelEmail)
        val tvUserEmail = currentView.findViewById<TextView>(R.id.tvUserEmail)
        val labelUid = currentView.findViewById<TextView>(R.id.labelUid)
        val tvUserUid = currentView.findViewById<TextView>(R.id.tvUserUid)
        val labelLocationSharing = currentView.findViewById<TextView>(R.id.labelLocationSharing)
        val locationSharingContainer = currentView.findViewById<View>(R.id.locationSharingContainer)
        val switchLocationSharing = currentView.findViewById<SwitchMaterial>(R.id.switchLocationSharing)
        val labelLocationCoords = currentView.findViewById<TextView>(R.id.labelLocationCoords)
        val tvLocationCoords = currentView.findViewById<TextView>(R.id.tvLocationCoords)
        val labelRadius = currentView.findViewById<TextView>(R.id.labelRadius)
        val radiusContainer = currentView.findViewById<View>(R.id.radiusContainer)
        val sliderRadius = currentView.findViewById<Slider>(R.id.sliderRadius)
        val tvRadiusValue = currentView.findViewById<TextView>(R.id.tvRadiusValue)
        val btnUpdateRange = currentView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUpdateRange)
        val tvChangePassword = currentView.findViewById<TextView>(R.id.tvChangePassword)
        val btnLogout = currentView.findViewById<Button>(R.id.btnLogout)
        val btnLogin = currentView.findViewById<Button>(R.id.btnLogin)
        val btnRegister = currentView.findViewById<Button>(R.id.btnRegister)

        if (user != null) {
            tvUserName.text = user.name
            tvUserEmail.text = user.email
            tvUserUid.text = user.uid
            
            labelName.visibility = View.VISIBLE
            tvUserName.visibility = View.VISIBLE
            labelEmail.visibility = View.VISIBLE
            tvUserEmail.visibility = View.VISIBLE
            labelUid.visibility = View.VISIBLE
            tvUserUid.visibility = View.VISIBLE
            labelLocationSharing.visibility = View.VISIBLE
            locationSharingContainer.visibility = View.VISIBLE
            tvChangePassword.visibility = View.VISIBLE
            btnLogout.visibility = View.VISIBLE
            btnLogin.visibility = View.GONE
            btnRegister.visibility = View.GONE

            // Load location sharing preference
            val locationSharingEnabled = PreferenceData.getInstance().getLocationSharingEnabled(context)
            switchLocationSharing.isChecked = locationSharingEnabled

            // Show location coordinates and radius slider if location sharing is enabled
            if (locationSharingEnabled) {
                val currentLocation = PreferenceData.getInstance().getCurrentLocation(context)
                if (currentLocation != null) {
                    labelLocationCoords.visibility = View.VISIBLE
                    tvLocationCoords.visibility = View.VISIBLE
                    tvLocationCoords.text = "Lat: ${String.format("%.6f", currentLocation.first)}\nLon: ${String.format("%.6f", currentLocation.second)}"
                    
                    labelRadius.visibility = View.VISIBLE
                    radiusContainer.visibility = View.VISIBLE
                    btnUpdateRange.visibility = View.VISIBLE
                    
                    // Find closest radius value index
                    val radiusValues = listOf(100, 200, 500, 1000, 5000, 10000)
                    val currentRadius = currentLocation.third.toInt()
                    val closestIndex = radiusValues.indexOfFirst { it >= currentRadius }
                        .takeIf { it != -1 } ?: (radiusValues.size - 1)
                    val indexToUse = if (closestIndex > 0 && 
                        kotlin.math.abs(radiusValues[closestIndex] - currentRadius) > 
                        kotlin.math.abs(radiusValues[closestIndex - 1] - currentRadius)) {
                        closestIndex - 1
                    } else {
                        closestIndex
                    }
                    
                    sliderRadius.value = indexToUse.toFloat()
                    tvRadiusValue.text = "${radiusValues[indexToUse]} m"
                } else {
                    labelLocationCoords.visibility = View.GONE
                    tvLocationCoords.visibility = View.GONE
                    labelRadius.visibility = View.GONE
                    radiusContainer.visibility = View.GONE
                    btnUpdateRange.visibility = View.GONE
                }
            } else {
                labelLocationCoords.visibility = View.GONE
                tvLocationCoords.visibility = View.GONE
                labelRadius.visibility = View.GONE
                radiusContainer.visibility = View.GONE
                btnUpdateRange.visibility = View.GONE
            }
        } else {
            labelName.visibility = View.GONE
            tvUserName.visibility = View.GONE
            labelEmail.visibility = View.GONE
            tvUserEmail.visibility = View.GONE
            labelUid.visibility = View.GONE
            tvUserUid.visibility = View.GONE
            labelLocationSharing.visibility = View.GONE
            locationSharingContainer.visibility = View.GONE
            labelLocationCoords.visibility = View.GONE
            tvLocationCoords.visibility = View.GONE
            labelRadius.visibility = View.GONE
            radiusContainer.visibility = View.GONE
            btnUpdateRange.visibility = View.GONE
            tvChangePassword.visibility = View.GONE
            btnLogout.visibility = View.GONE
            btnLogin.visibility = View.VISIBLE
            btnRegister.visibility = View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        val currentView = view ?: return
        val btnLogin = currentView.findViewById<Button>(R.id.btnLogin)
        val btnRegister = currentView.findViewById<Button>(R.id.btnRegister)
        val btnLogout = currentView.findViewById<Button>(R.id.btnLogout)

        btnLogin.setOnClickListener {
            findNavController().navigate(R.id.prihlasenieFragment)
        }

        btnRegister.setOnClickListener {
          findNavController().navigate(R.id.signupFragment)
        }


        btnLogout.setOnClickListener {
            val currentUser = PreferenceData.getInstance().getUser(context)
            if (currentUser != null) {
                Log.d("ProfileFragment", "Logout clicked - User found: name=${currentUser.name}, email=${currentUser.email}")
                Log.d("ProfileFragment", "Access token: '${currentUser.access}', length: ${currentUser.access.length}, isEmpty: ${currentUser.access.isEmpty()}")
                
                if (currentUser.access.isNotEmpty()) {
                    Log.d("ProfileFragment", "Calling API logout with access token")
                    viewModel.logout(currentUser.access)
                } else {
                    Log.w("ProfileFragment", "Access token is empty - clearing local data only (no API call)")
                    PreferenceData.getInstance().clearData(context)
                    updateUI()
                    Snackbar.make(
                        currentView.findViewById(R.id.btnLogout),
                        getString(R.string.logout_success),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    findNavController().navigate(R.id.prihlasenieFragment)
                }
            } else {
                Log.w("ProfileFragment", "No user found - cannot logout")
                Snackbar.make(
                    currentView.findViewById(R.id.btnLogout),
                    getString(R.string.no_user_logged_in),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupLocationSharingToggle() {
        val currentView = view ?: return
        val switchLocationSharing = currentView.findViewById<SwitchMaterial>(R.id.switchLocationSharing)

        switchLocationSharing.setOnCheckedChangeListener { _, isChecked ->
            val user = PreferenceData.getInstance().getUser(context)
            if (user == null || user.access.isEmpty()) {
                switchLocationSharing.isChecked = false
                Snackbar.make(
                    switchLocationSharing,
                    getString(R.string.no_user_logged_in),
                    Snackbar.LENGTH_SHORT
                ).show()
                return@setOnCheckedChangeListener
            }

            if (isChecked) {
                // Request location permission and get location
                if (checkLocationPermission()) {
                    getCurrentLocationAndUpdateGeofence(user.access)
                } else {
                    requestLocationPermission()
                    switchLocationSharing.isChecked = false
                }
            } else {
                // Delete geofence
                PreferenceData.getInstance().setLocationSharingEnabled(context, false)
                viewModel.deleteGeofence(user.access)
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val user = PreferenceData.getInstance().getUser(context)
                if (user != null && user.access.isNotEmpty()) {
                    val switchLocationSharing = view?.findViewById<SwitchMaterial>(R.id.switchLocationSharing)
                    switchLocationSharing?.isChecked = true
                    getCurrentLocationAndUpdateGeofence(user.access)
                }
            } else {
                val currentView = view ?: return
                Snackbar.make(
                    currentView.findViewById(R.id.switchLocationSharing),
                    getString(R.string.location_permission_denied),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun getCurrentLocationAndUpdateGeofence(accessToken: String) {
        if (!checkLocationPermission()) {
            return
        }

        // Request current location with high priority
        val locationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setDurationMillis(10000)
            .setMaxUpdateAgeMillis(5000)
            .build()

        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(locationRequest, cancellationTokenSource.token)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Log.d("ProfileFragment", "Current location: lat=${location.latitude}, lon=${location.longitude}")
                    PreferenceData.getInstance().setLocationSharingEnabled(context, true)
                    PreferenceData.getInstance().setCurrentLocation(context, location.latitude, location.longitude, DEFAULT_RADIUS)
                    viewModel.updateGeofence(accessToken, location.latitude, location.longitude, DEFAULT_RADIUS)
                    updateUI() // Refresh UI to show coordinates
                } else {
                    Log.e("ProfileFragment", "Location is null - trying lastLocation as fallback")
                    // Fallback to lastLocation if getCurrentLocation returns null
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation: Location? ->
                        if (lastLocation != null) {
                            Log.d("ProfileFragment", "Last known location: lat=${lastLocation.latitude}, lon=${lastLocation.longitude}")
                            PreferenceData.getInstance().setLocationSharingEnabled(context, true)
                            PreferenceData.getInstance().setCurrentLocation(context, lastLocation.latitude, lastLocation.longitude, DEFAULT_RADIUS)
                            viewModel.updateGeofence(accessToken, lastLocation.latitude, lastLocation.longitude, DEFAULT_RADIUS)
                            updateUI()
                        } else {
                            Log.e("ProfileFragment", "Last location is also null")
                            val currentView = view ?: return@addOnSuccessListener
                            val switchLocationSharing = currentView.findViewById<SwitchMaterial>(R.id.switchLocationSharing)
                            switchLocationSharing.isChecked = false
                            PreferenceData.getInstance().setLocationSharingEnabled(context, false)
                            Snackbar.make(
                                switchLocationSharing,
                                getString(R.string.location_not_available),
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }.addOnFailureListener { exception ->
                Log.e("ProfileFragment", "Failed to get current location: ${exception.message}", exception)
                val currentView = view ?: return@addOnFailureListener
                val switchLocationSharing = currentView.findViewById<SwitchMaterial>(R.id.switchLocationSharing)
                switchLocationSharing.isChecked = false
                PreferenceData.getInstance().setLocationSharingEnabled(context, false)
                Snackbar.make(
                    switchLocationSharing,
                    getString(R.string.location_error),
                    Snackbar.LENGTH_LONG
                ).show()
            }
    }

    private fun setupChangePasswordClick() {
        val currentView = view ?: return
        val tvChangePassword = currentView.findViewById<TextView>(R.id.tvChangePassword)
        
        tvChangePassword.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_change_password)
        }
    }

    private fun setupRadiusSlider() {
        val currentView = view ?: return
        val sliderRadius = currentView.findViewById<Slider>(R.id.sliderRadius)
        val tvRadiusValue = currentView.findViewById<TextView>(R.id.tvRadiusValue)

        // Define radius values: 100, 200, 500, 1000, 5000, 10000
        val radiusValues = listOf(100, 200, 500, 1000, 5000, 10000)

        // Set yellow color for active track and thumb
        val yellowColor = ContextCompat.getColor(requireContext(), R.color.secondary_yellow)
        val yellowColorStateList = ColorStateList.valueOf(yellowColor)
        sliderRadius.thumbTintList = yellowColorStateList
        
        // Set red color for inactive track
        val redColor = ContextCompat.getColor(requireContext(), R.color.accent_red)
        sliderRadius.trackInactiveTintList = ColorStateList.valueOf(redColor)
        
        // Set yellow color for active track
        sliderRadius.trackActiveTintList = yellowColorStateList

        // Set label formatter to show actual radius values
        sliderRadius.setLabelFormatter { value ->
            val index = value.toInt().coerceIn(0, radiusValues.size - 1)
            "${radiusValues[index]} m"
        }

        sliderRadius.addOnChangeListener { _, value, _ ->
            val index = value.toInt().coerceIn(0, radiusValues.size - 1)
            tvRadiusValue.text = "${radiusValues[index]} m"
        }
    }

    private fun setupUpdateRangeButton() {
        val currentView = view ?: return
        val btnUpdateRange = currentView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUpdateRange)
        val sliderRadius = currentView.findViewById<Slider>(R.id.sliderRadius)

        // Define radius values: 100, 200, 500, 1000, 5000, 10000
        val radiusValues = listOf(100, 200, 500, 1000, 5000, 10000)

        btnUpdateRange.setOnClickListener {
            val user = PreferenceData.getInstance().getUser(context)
            if (user == null || user.access.isEmpty()) {
                Snackbar.make(
                    btnUpdateRange,
                    getString(R.string.no_user_logged_in),
                    Snackbar.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val currentLocation = PreferenceData.getInstance().getCurrentLocation(context)
            if (currentLocation == null) {
                Snackbar.make(
                    btnUpdateRange,
                    getString(R.string.location_not_available),
                    Snackbar.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Get radius value from slider index
            val index = sliderRadius.value.toInt().coerceIn(0, radiusValues.size - 1)
            val newRadius = radiusValues[index].toDouble()
            PreferenceData.getInstance().setCurrentLocation(context, currentLocation.first, currentLocation.second, newRadius)
            viewModel.updateGeofence(user.access, currentLocation.first, currentLocation.second, newRadius)
        }
    }
}

