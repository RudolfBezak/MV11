package com.example.mv11

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.content.res.ColorStateList
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.example.mv11.databinding.FragmentProfileBinding

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var binding: FragmentProfileBinding? = null
    private lateinit var viewModel: AuthViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val DEFAULT_RADIUS = 100.0
    private var currentPhotoUri: Uri? = null
    private var tempImageFile: File? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadImage(uri)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[AuthViewModel::class.java]

        binding = FragmentProfileBinding.bind(view).apply {
            lifecycleOwner = viewLifecycleOwner
            this.viewModel = this@ProfileFragment.viewModel
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        updateUI()

        viewModel.logoutResult.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { result ->
                binding?.let { bnd ->
                    if (result.second) {
                        Log.d("ProfileFragment", "Logout successful")
                        PreferenceData.getInstance().clearData(context)
                        updateUI()
                        Snackbar.make(
                            bnd.btnLogout,
                            getString(R.string.logout_success),
                            Snackbar.LENGTH_SHORT
                        ).show()
                        findNavController().navigate(R.id.prihlasenieFragment)
                    } else {
                        Log.e("ProfileFragment", "Logout failed: ${result.first}")
                        Snackbar.make(
                            bnd.btnLogout,
                            "${getString(R.string.logout_error)}: ${result.first}",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        viewModel.geofenceUpdateResult.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { result ->
                binding?.let { bnd ->
                    if (result.second) {
                        Log.d("ProfileFragment", "Geofence updated successfully")
                        val currentLocation = PreferenceData.getInstance().getCurrentLocation(context)
                        if (currentLocation != null) {
                            updateUI()
                        }
                        val targetView = bnd.btnUpdateRange ?: bnd.switchLocationSharing
                        Snackbar.make(
                            targetView,
                            getString(R.string.radius_updated),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } else {
                        Log.e("ProfileFragment", "Geofence update failed: ${result.first}")
                        bnd.switchLocationSharing.isChecked = false
                        PreferenceData.getInstance().setLocationSharingEnabled(context, false)
                        PreferenceData.getInstance().clearCurrentLocation(context)
                        updateUI()
                        Snackbar.make(
                            bnd.switchLocationSharing,
                            result.first,
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        viewModel.geofenceDeleteResult.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { result ->
                binding?.let { bnd ->
                    if (result.second) {
                        Log.d("ProfileFragment", "Geofence deleted successfully")
                        PreferenceData.getInstance().clearCurrentLocation(context)
                        updateUI()
                        Snackbar.make(
                            bnd.switchLocationSharing,
                            getString(R.string.location_sharing_disabled),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } else {
                        Log.e("ProfileFragment", "Geofence delete failed: ${result.first}")
                        Snackbar.make(
                            bnd.switchLocationSharing,
                            result.first,
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
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
        
        binding?.bottomNavigationWidget?.setActiveItem(BottomNavItem.PROFILE)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val bnd = binding ?: return
        val currentUser = PreferenceData.getInstance().getUser(context)
        
        val userId = arguments?.getString("userId") ?: currentUser?.uid ?: ""
        val isOwnProfile = userId.isEmpty() || userId == currentUser?.uid

        if (isOwnProfile && currentUser != null) {
            bnd.textView.text = getString(R.string.your_profile)
            
            bnd.tvUserName.text = currentUser.name
            bnd.tvUserEmail.text = currentUser.email
            bnd.tvUserUid.text = currentUser.uid
            
            bnd.ivUserPhoto.visibility = View.VISIBLE
            bnd.btnUploadPhoto.visibility = View.VISIBLE
            bnd.btnDeletePhoto.visibility = View.VISIBLE
            
            bnd.labelName.visibility = View.VISIBLE
            bnd.tvUserName.visibility = View.VISIBLE
            bnd.labelEmail.visibility = View.VISIBLE
            bnd.tvUserEmail.visibility = View.VISIBLE
            bnd.labelUid.visibility = View.VISIBLE
            bnd.tvUserUid.visibility = View.VISIBLE
            bnd.labelLocationSharing.visibility = View.VISIBLE
            bnd.locationSharingContainer.visibility = View.VISIBLE
            bnd.tvChangePassword.visibility = View.VISIBLE
            bnd.btnLogout.visibility = View.VISIBLE
            bnd.btnLogin.visibility = View.GONE
            bnd.btnRegister.visibility = View.GONE
            
            if (currentUser.access.isNotEmpty()) {
                viewModel.getUserProfile(currentUser.access, currentUser.uid)
            }

            val locationSharingEnabled = PreferenceData.getInstance().getLocationSharingEnabled(context)
            bnd.switchLocationSharing.isChecked = locationSharingEnabled

            if (locationSharingEnabled) {
                val currentLocation = PreferenceData.getInstance().getCurrentLocation(context)
                if (currentLocation != null) {
                    bnd.labelLocationCoords.visibility = View.VISIBLE
                    bnd.tvLocationCoords.visibility = View.VISIBLE
                    bnd.tvLocationCoords.text = "Lat: ${String.format("%.6f", currentLocation.first)}\nLon: ${String.format("%.6f", currentLocation.second)}"
                    
                    bnd.labelRadius.visibility = View.VISIBLE
                    bnd.radiusContainer.visibility = View.VISIBLE
                    bnd.btnUpdateRange.visibility = View.VISIBLE
                    
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
                    
                    bnd.sliderRadius.value = indexToUse.toFloat()
                    bnd.tvRadiusValue.text = "${radiusValues[indexToUse]} m"
                } else {
                    bnd.labelLocationCoords.visibility = View.GONE
                    bnd.tvLocationCoords.visibility = View.GONE
                    bnd.labelRadius.visibility = View.GONE
                    bnd.radiusContainer.visibility = View.GONE
                    bnd.btnUpdateRange.visibility = View.GONE
                }
            } else {
                bnd.labelLocationCoords.visibility = View.GONE
                bnd.tvLocationCoords.visibility = View.GONE
                bnd.labelRadius.visibility = View.GONE
                bnd.radiusContainer.visibility = View.GONE
                bnd.btnUpdateRange.visibility = View.GONE
            }
        } else if (currentUser != null && userId.isNotEmpty() && userId != currentUser.uid) {
            bnd.textView.text = getString(R.string.user_profile)
            
            if (currentUser.access.isNotEmpty()) {
                viewModel.getUserProfile(currentUser.access, userId)
            }
            
            bnd.ivUserPhoto.visibility = View.VISIBLE
            bnd.btnUploadPhoto.visibility = View.GONE
            bnd.btnDeletePhoto.visibility = View.GONE
            
            bnd.labelName.visibility = View.VISIBLE
            bnd.tvUserName.visibility = View.VISIBLE
            bnd.labelEmail.visibility = View.GONE
            bnd.tvUserEmail.visibility = View.GONE
            bnd.labelUid.visibility = View.VISIBLE
            bnd.tvUserUid.visibility = View.VISIBLE
            
            bnd.labelLocationSharing.visibility = View.GONE
            bnd.locationSharingContainer.visibility = View.GONE
            bnd.tvChangePassword.visibility = View.GONE
            bnd.btnLogout.visibility = View.GONE
            bnd.btnLogin.visibility = View.GONE
            bnd.btnRegister.visibility = View.GONE
            bnd.labelLocationCoords.visibility = View.GONE
            bnd.tvLocationCoords.visibility = View.GONE
            bnd.labelRadius.visibility = View.GONE
            bnd.radiusContainer.visibility = View.GONE
            bnd.btnUpdateRange.visibility = View.GONE
        } else if (currentUser != null) {
            bnd.textView.text = getString(R.string.your_profile)
            
            bnd.tvUserName.text = currentUser.name
            bnd.tvUserEmail.text = currentUser.email
            bnd.tvUserUid.text = currentUser.uid
            
            bnd.ivUserPhoto.visibility = View.VISIBLE
            bnd.btnUploadPhoto.visibility = View.VISIBLE
            bnd.btnDeletePhoto.visibility = View.VISIBLE
            
            bnd.labelName.visibility = View.VISIBLE
            bnd.tvUserName.visibility = View.VISIBLE
            bnd.labelEmail.visibility = View.VISIBLE
            bnd.tvUserEmail.visibility = View.VISIBLE
            bnd.labelUid.visibility = View.VISIBLE
            bnd.tvUserUid.visibility = View.VISIBLE
            bnd.labelLocationSharing.visibility = View.VISIBLE
            bnd.locationSharingContainer.visibility = View.VISIBLE
            bnd.tvChangePassword.visibility = View.VISIBLE
            bnd.btnLogout.visibility = View.VISIBLE
            bnd.btnLogin.visibility = View.GONE
            bnd.btnRegister.visibility = View.GONE
            
            if (currentUser.access.isNotEmpty()) {
                viewModel.getUserProfile(currentUser.access, currentUser.uid)
            }
            
            val locationSharingEnabled = PreferenceData.getInstance().getLocationSharingEnabled(context)
            bnd.switchLocationSharing.isChecked = locationSharingEnabled
            
            bnd.labelLocationCoords.visibility = View.GONE
            bnd.tvLocationCoords.visibility = View.GONE
            bnd.labelRadius.visibility = View.GONE
            bnd.radiusContainer.visibility = View.GONE
            bnd.btnUpdateRange.visibility = View.GONE
        } else {
            bnd.ivUserPhoto.visibility = View.GONE
            bnd.btnUploadPhoto.visibility = View.GONE
            bnd.btnDeletePhoto.visibility = View.GONE
            
            bnd.labelName.visibility = View.GONE
            bnd.tvUserName.visibility = View.GONE
            bnd.labelEmail.visibility = View.GONE
            bnd.tvUserEmail.visibility = View.GONE
            bnd.labelUid.visibility = View.GONE
            bnd.tvUserUid.visibility = View.GONE
            bnd.labelLocationSharing.visibility = View.GONE
            bnd.locationSharingContainer.visibility = View.GONE
            bnd.labelLocationCoords.visibility = View.GONE
            bnd.tvLocationCoords.visibility = View.GONE
            bnd.labelRadius.visibility = View.GONE
            bnd.radiusContainer.visibility = View.GONE
            bnd.btnUpdateRange.visibility = View.GONE
            bnd.tvChangePassword.visibility = View.GONE
            bnd.btnLogout.visibility = View.GONE
            bnd.btnLogin.visibility = View.VISIBLE
            bnd.btnRegister.visibility = View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        binding?.let { bnd ->
            bnd.btnLogin.setOnClickListener {
                findNavController().navigate(R.id.prihlasenieFragment)
            }

            bnd.btnRegister.setOnClickListener {
                findNavController().navigate(R.id.signupFragment)
            }

            bnd.btnLogout.setOnClickListener {
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
                            bnd.btnLogout,
                            getString(R.string.logout_success),
                            Snackbar.LENGTH_SHORT
                        ).show()
                        findNavController().navigate(R.id.prihlasenieFragment)
                    }
                } else {
                    Log.w("ProfileFragment", "No user found - cannot logout")
                    Snackbar.make(
                        bnd.btnLogout,
                        getString(R.string.no_user_logged_in),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupLocationSharingToggle() {
        binding?.switchLocationSharing?.setOnCheckedChangeListener { _, isChecked ->
            val user = PreferenceData.getInstance().getUser(context)
            binding?.let { bnd ->
                if (user == null || user.access.isEmpty()) {
                    bnd.switchLocationSharing.isChecked = false
                    Snackbar.make(
                        bnd.switchLocationSharing,
                        getString(R.string.no_user_logged_in),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@setOnCheckedChangeListener
                }

                if (isChecked) {
                    if (checkLocationPermission()) {
                        getCurrentLocationAndUpdateGeofence(user.access)
                    } else {
                        requestLocationPermission()
                        bnd.switchLocationSharing.isChecked = false
                    }
                } else {
                    PreferenceData.getInstance().setLocationSharingEnabled(context, false)
                    viewModel.deleteGeofence(user.access)
                }
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
            binding?.let { bnd ->
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val user = PreferenceData.getInstance().getUser(context)
                    if (user != null && user.access.isNotEmpty()) {
                        bnd.switchLocationSharing.isChecked = true
                        getCurrentLocationAndUpdateGeofence(user.access)
                    }
                } else {
                    Snackbar.make(
                        bnd.switchLocationSharing,
                        getString(R.string.location_permission_denied),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun getCurrentLocationAndUpdateGeofence(accessToken: String) {
        if (!checkLocationPermission()) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            binding?.let { bnd ->
                if (location != null) {
                    Log.d("ProfileFragment", "Location: lat=${location.latitude}, lon=${location.longitude}")
                    PreferenceData.getInstance().setLocationSharingEnabled(context, true)
                    PreferenceData.getInstance().setCurrentLocation(context, location.latitude, location.longitude, DEFAULT_RADIUS)
                    viewModel.updateGeofence(accessToken, location.latitude, location.longitude, DEFAULT_RADIUS)
                    updateUI()
                } else {
                    Log.e("ProfileFragment", "Location is null")
                    bnd.switchLocationSharing.isChecked = false
                    PreferenceData.getInstance().setLocationSharingEnabled(context, false)
                    Snackbar.make(
                        bnd.switchLocationSharing,
                        getString(R.string.location_not_available),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("ProfileFragment", "Failed to get location: ${exception.message}", exception)
            binding?.let { bnd ->
                bnd.switchLocationSharing.isChecked = false
                PreferenceData.getInstance().setLocationSharingEnabled(context, false)
                Snackbar.make(
                    bnd.switchLocationSharing,
                    getString(R.string.location_error),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupChangePasswordClick() {
        binding?.tvChangePassword?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_change_password)
        }
    }

    private fun setupRadiusSlider() {
        binding?.let { bnd ->
            val yellowColor = ContextCompat.getColor(requireContext(), R.color.secondary_yellow)
            val yellowColorStateList = ColorStateList.valueOf(yellowColor)
            bnd.sliderRadius.thumbTintList = yellowColorStateList
            
            val redColor = ContextCompat.getColor(requireContext(), R.color.accent_red)
            bnd.sliderRadius.trackInactiveTintList = ColorStateList.valueOf(redColor)
            bnd.sliderRadius.trackActiveTintList = yellowColorStateList

            val radiusValues = listOf(100, 200, 500, 1000, 5000, 10000)

            bnd.sliderRadius.setLabelFormatter { value ->
                val index = value.toInt().coerceIn(0, radiusValues.size - 1)
                "${radiusValues[index]} m"
            }

            bnd.sliderRadius.addOnChangeListener { _, value, _ ->
                val index = value.toInt().coerceIn(0, radiusValues.size - 1)
                bnd.tvRadiusValue.text = "${radiusValues[index]} m"
            }
        }
    }

    private fun setupUpdateRangeButton() {
        binding?.let { bnd ->
            bnd.btnUpdateRange.setOnClickListener {
                val user = PreferenceData.getInstance().getUser(context)
                if (user == null || user.access.isEmpty()) {
                    Snackbar.make(
                        bnd.btnUpdateRange,
                        getString(R.string.no_user_logged_in),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val currentLocation = PreferenceData.getInstance().getCurrentLocation(context)
                if (currentLocation == null) {
                    Snackbar.make(
                        bnd.btnUpdateRange,
                        getString(R.string.location_not_available),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val radiusValues = listOf(100, 200, 500, 1000, 5000, 10000)
                val index = bnd.sliderRadius.value.toInt().coerceIn(0, radiusValues.size - 1)
                val newRadius = radiusValues[index].toDouble()
                Log.d("ProfileFragment", "Updating radius: index=$index, radius=$newRadius")
                PreferenceData.getInstance().setCurrentLocation(context, currentLocation.first, currentLocation.second, newRadius)
                viewModel.updateGeofence(user.access, currentLocation.first, currentLocation.second, newRadius)
            }
        }
    }

    private fun setupPhotoButtons() {
        binding?.let { bnd ->
            bnd.btnUploadPhoto.setOnClickListener {
                pickImage()
            }

            bnd.btnDeletePhoto.setOnClickListener {
                val user = PreferenceData.getInstance().getUser(context)
                if (user != null && user.access.isNotEmpty()) {
                    viewModel.deletePhoto(user.access)
                }
            }
        }
    }

    private fun observePhotoResults() {
        viewModel.userProfileResult.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { result ->
                binding?.let { bnd ->
                    if (result.second != null) {
                        val profile = result.second!!
                        loadUserPhoto(profile.photo)
                        
                        val userId = arguments?.getString("userId") ?: ""
                        val currentUser = PreferenceData.getInstance().getUser(context)
                        val isOwnProfile = userId.isEmpty() || userId == currentUser?.uid
                        
                        if (!isOwnProfile) {
                            bnd.tvUserName.text = profile.name
                            bnd.labelEmail.visibility = View.GONE
                            bnd.tvUserEmail.visibility = View.GONE
                            bnd.tvUserUid.text = profile.id
                        }
                    }
                }
            }
        }

        viewModel.photoUploadResult.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { result ->
                binding?.let { bnd ->
                    if (result.second != null) {
                        loadUserPhoto(result.second!!.photo)
                        Snackbar.make(
                            bnd.btnUploadPhoto,
                            getString(R.string.photo_uploaded),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } else {
                        Snackbar.make(
                            bnd.btnUploadPhoto,
                            result.first.ifEmpty { getString(R.string.photo_upload_failed) },
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        viewModel.photoDeleteResult.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { result ->
                binding?.let { bnd ->
                    if (result.second != null) {
                        loadUserPhoto("")
                        Snackbar.make(
                            bnd.btnDeletePhoto,
                            getString(R.string.photo_deleted),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } else {
                        Snackbar.make(
                            bnd.btnDeletePhoto,
                            result.first.ifEmpty { getString(R.string.photo_delete_failed) },
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
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
            binding?.let { bnd ->
                Snackbar.make(
                    bnd.btnUploadPhoto,
                    getString(R.string.photo_upload_failed),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadUserPhoto(photoPath: String) {
        binding?.let { bnd ->
            if (photoPath.isNotEmpty()) {
                val cleanPhotoPath = photoPath.replace("../", "")
                val photoUrl = "https://upload.mcomputing.eu/$cleanPhotoPath"
                Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.profile_foreground)
                    .error(R.drawable.profile_foreground)
                    .circleCrop()
                    .into(bnd.ivUserPhoto)
            } else {
                Glide.with(this).clear(bnd.ivUserPhoto)
                bnd.ivUserPhoto.setImageResource(R.drawable.profile_foreground)
            }
        }
    }
}

