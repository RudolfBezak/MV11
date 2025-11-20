package com.example.mv11

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.mv11.databinding.FragmentProfileBinding
import kotlinx.coroutines.launch

/**
 * ProfileFragment - Fragment pre zobrazenie profilu používateľa.
 * 
 * Obsahuje:
 * - Switch pre zdieľanie polohy
 * - GPS povolenia handling
 * - DataBinding s ProfileViewModel
 */
class ProfileFragment : Fragment() {

    private var binding: FragmentProfileBinding? = null
    private lateinit var viewModel: ProfileViewModel
    private lateinit var repository: DataRepository

    /**
     * Pole povolení potrebných pre GPS.
     */
    private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    /**
     * ActivityResultLauncher pre požiadavku na GPS povolenie.
     * 
     * registerForActivityResult - moderný spôsob požiadavky o povolenia v Android.
     * Nahradzuje starý onRequestPermissionsResult callback.
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("ProfileFragment", "GPS permission granted")
            // Používateľ povolil GPS - ulož stav do SharedPreferences
            PreferenceData.getInstance().putSharing(requireContext(), true)
            viewModel.updateSharingLocation(true)
        } else {
            Log.d("ProfileFragment", "GPS permission denied")
            // Používateľ zamietol GPS - vypni sharing
            PreferenceData.getInstance().putSharing(requireContext(), false)
            viewModel.updateSharingLocation(false)
        }
    }

    /**
     * Kontroluje či má aplikácia všetky potrebné povolenia.
     * 
     * @param context - kontext aplikácie
     * @return true ak má všetky povolenia, false ak nie
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
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializácia Repository a ViewModelu
        repository = DataRepository.getInstance(requireContext())
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(repository) as T
            }
        })[ProfileViewModel::class.java]

        // Nastavenie DataBinding
        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
            this.viewModel = viewModel
        }

        // Načítaj uložený stav zdieľania polohy
        val savedSharing = PreferenceData.getInstance().getSharing(requireContext())
        viewModel.sharingLocation.postValue(savedSharing)

        // Pozorovanie zmien v sharingLocation
        viewModel.sharingLocation.observe(viewLifecycleOwner) { sharing ->
            sharing?.let {
                if (it) {
                    // Používateľ chce zdieľať polohu
                    if (!hasPermissions(requireContext())) {
                        // Nemá povolenie - požiadaj o ne
                        Log.d("ProfileFragment", "Requesting GPS permission")
                        viewModel.sharingLocation.postValue(false)  // Dočasne vypni switch
                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        // Má povolenie - ulož stav
                        Log.d("ProfileFragment", "GPS permission already granted, saving state")
                        PreferenceData.getInstance().putSharing(requireContext(), true)
                    }
                } else {
                    // Používateľ nechce zdieľať polohu
                    Log.d("ProfileFragment", "User disabled location sharing")
                    PreferenceData.getInstance().putSharing(requireContext(), false)
                    
                    // Odoslať DELETE request na server pre odstránenie polohy
                    lifecycleScope.launch {
                        val error = repository.apiDeleteGeofence()
                        if (error.isNotEmpty()) {
                            Log.e("ProfileFragment", "Failed to delete geofence: $error")
                        } else {
                            Log.d("ProfileFragment", "Geofence deleted successfully")
                        }
                    }
                }
            }
        }

        val bottomNav = view.findViewById<BottomNavigationWidget>(R.id.bottomNavigationWidget)
        bottomNav.setActiveItem(BottomNavItem.PROFILE)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
