package com.example.mv11

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar

class ProfileFragment : Fragment() {

    private lateinit var viewModel: AuthViewModel
    private var view: View? = null

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

        setupClickListeners()
        setupLocationSharingToggle()
        setupChangePasswordClick()

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
        } else {
            labelName.visibility = View.GONE
            tvUserName.visibility = View.GONE
            labelEmail.visibility = View.GONE
            tvUserEmail.visibility = View.GONE
            labelUid.visibility = View.GONE
            tvUserUid.visibility = View.GONE
            labelLocationSharing.visibility = View.GONE
            locationSharingContainer.visibility = View.GONE
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
            PreferenceData.getInstance().setLocationSharingEnabled(context, isChecked)
            Log.d("ProfileFragment", "Location sharing ${if (isChecked) "enabled" else "disabled"}")
        }
    }

    private fun setupChangePasswordClick() {
        val currentView = view ?: return
        val tvChangePassword = currentView.findViewById<TextView>(R.id.tvChangePassword)
        
        tvChangePassword.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_change_password)
        }
    }
}

