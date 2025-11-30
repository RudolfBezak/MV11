package com.example.mv11

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.content.res.ColorStateList
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
    private var currentPhotoUri: Uri? = null
    private var tempImageFile: File? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadImage(uri)
            }
        }
    }

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
        setupPhotoButtons()
        observePhotoResults()
        
        // Load user photo on fragment creation
        val user = PreferenceData.getInstance().getUser(context)
        if (user != null && user.access.isNotEmpty()) {
            viewModel.getUserProfile(user.access, user.uid)
        }

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
        val ivUserPhoto = currentView.findViewById<ImageView>(R.id.ivUserPhoto)
        val btnUploadPhoto = currentView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUploadPhoto)
        val btnDeletePhoto = currentView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDeletePhoto)

        if (user != null) {
            tvUserName.text = user.name
            tvUserEmail.text = user.email
            tvUserUid.text = user.uid
            
            // Show photo and buttons
            ivUserPhoto.visibility = View.VISIBLE
            btnUploadPhoto.visibility = View.VISIBLE
            btnDeletePhoto.visibility = View.VISIBLE
            
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
            
            // Load user photo
            if (user.access.isNotEmpty()) {
                viewModel.getUserProfile(user.access, user.uid)
            }

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
            ivUserPhoto.visibility = View.GONE
            btnUploadPhoto.visibility = View.GONE
            btnDeletePhoto.visibility = View.GONE
            
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

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                Log.d("ProfileFragment", "Location: lat=${location.latitude}, lon=${location.longitude}")
                PreferenceData.getInstance().setLocationSharingEnabled(context, true)
                PreferenceData.getInstance().setCurrentLocation(context, location.latitude, location.longitude, DEFAULT_RADIUS)
                viewModel.updateGeofence(accessToken, location.latitude, location.longitude, DEFAULT_RADIUS)
                updateUI() // Refresh UI to show coordinates
            } else {
                Log.e("ProfileFragment", "Location is null")
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
        }.addOnFailureListener { exception ->
            Log.e("ProfileFragment", "Failed to get location: ${exception.message}", exception)
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

        // Set yellow color for active track and thumb
        val yellowColor = ContextCompat.getColor(requireContext(), R.color.secondary_yellow)
        val yellowColorStateList = ColorStateList.valueOf(yellowColor)
        sliderRadius.thumbTintList = yellowColorStateList
        
        // Set red color for inactive track
        val redColor = ContextCompat.getColor(requireContext(), R.color.accent_red)
        sliderRadius.trackInactiveTintList = ColorStateList.valueOf(redColor)
        
        // Set yellow color for active track
        sliderRadius.trackActiveTintList = yellowColorStateList

        // Define radius values: 100, 200, 500, 1000, 5000, 10000
        val radiusValues = listOf(100, 200, 500, 1000, 5000, 10000)

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

            // Define radius values: 100, 200, 500, 1000, 5000, 10000
            val radiusValues = listOf(100, 200, 500, 1000, 5000, 10000)
            
            // Get radius value from slider index
            val index = sliderRadius.value.toInt().coerceIn(0, radiusValues.size - 1)
            val newRadius = radiusValues[index].toDouble()
            Log.d("ProfileFragment", "Updating radius: index=$index, radius=$newRadius")
            PreferenceData.getInstance().setCurrentLocation(context, currentLocation.first, currentLocation.second, newRadius)
            viewModel.updateGeofence(user.access, currentLocation.first, currentLocation.second, newRadius)
        }
    }

    private fun setupPhotoButtons() {
        val currentView = view ?: return
        val btnUploadPhoto = currentView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUploadPhoto)
        val btnDeletePhoto = currentView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDeletePhoto)

        btnUploadPhoto.setOnClickListener {
            pickImage()
        }

        btnDeletePhoto.setOnClickListener {
            val user = PreferenceData.getInstance().getUser(context)
            if (user != null && user.access.isNotEmpty()) {
                viewModel.deletePhoto(user.access)
            }
        }
    }

    private fun observePhotoResults() {
        viewModel.userProfileResult.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { result ->
                if (result.second != null) {
                    loadUserPhoto(result.second!!.photo)
                }
            }
        }

        viewModel.photoUploadResult.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { result ->
                val currentView = view ?: return@observe
                if (result.second != null) {
                    loadUserPhoto(result.second!!.photo)
                    Snackbar.make(
                        currentView.findViewById(R.id.btnUploadPhoto),
                        getString(R.string.photo_uploaded),
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    Snackbar.make(
                        currentView.findViewById(R.id.btnUploadPhoto),
                        result.first.ifEmpty { getString(R.string.photo_upload_failed) },
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        viewModel.photoDeleteResult.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { result ->
                val currentView = view ?: return@observe
                if (result.second != null) {
                    loadUserPhoto("") // Clear photo
                    Snackbar.make(
                        currentView.findViewById(R.id.btnDeletePhoto),
                        getString(R.string.photo_deleted),
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    Snackbar.make(
                        currentView.findViewById(R.id.btnDeletePhoto),
                        result.first.ifEmpty { getString(R.string.photo_delete_failed) },
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun uploadImage(uri: Uri) {
        val user = PreferenceData.getInstance().getUser(context)
        if (user == null || user.access.isEmpty()) {
            return
        }

        try {
            // Convert URI to File
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val tempFile = File(requireContext().cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(tempFile)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            tempImageFile = tempFile
            viewModel.uploadPhoto(user.access, tempFile)
        } catch (e: IOException) {
            Log.e("ProfileFragment", "Error converting image: ${e.message}")
            val currentView = view ?: return
            Snackbar.make(
                currentView.findViewById(R.id.btnUploadPhoto),
                getString(R.string.photo_upload_failed),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun loadUserPhoto(photoPath: String) {
        val currentView = view ?: return
        val ivUserPhoto = currentView.findViewById<ImageView>(R.id.ivUserPhoto)
        
        if (photoPath.isNotEmpty()) {
            val cleanPhotoPath = photoPath.replace("../", "")
            val photoUrl = "https://upload.mcomputing.eu/$cleanPhotoPath"
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.profile_foreground)
                .error(R.drawable.profile_foreground)
                .circleCrop()
                .into(ivUserPhoto)
        } else {
            Glide.with(this).clear(ivUserPhoto)
            ivUserPhoto.setImageResource(R.drawable.profile_foreground)
        }
    }
}

