package com.example.mv11

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class SignupFragment : Fragment() {

    private lateinit var viewModel: AuthViewModel
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signup, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[AuthViewModel::class.java]

        viewModel.registrationResult.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { result ->
                result.second?.let { user ->
                    Log.d("SignupFragment", "Registration successful: $user")
                    Log.d("SignupFragment", "Access token: ${user.access}, length: ${user.access.length}")
                    Log.d("SignupFragment", "Refresh token: ${user.refresh}, length: ${user.refresh.length}")

                    if (user.access.isNotEmpty()) {
                        PreferenceData.getInstance().putUser(context, user)
                        Log.d("SignupFragment", "User saved to SharedPreferences with access token")
                        
                        Snackbar.make(
                            view.findViewById(R.id.submitButton),
                            "Registration successful!",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        findNavController().navigate(R.id.feedFragment)
                    } else {
                        Log.w("SignupFragment", "Access token is empty after registration")
                        Snackbar.make(
                            view.findViewById(R.id.submitButton),
                            "Registration successful! Please login to continue.",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        findNavController().navigate(R.id.prihlasenieFragment)
                    }
                } ?: run {
                    Log.e("SignupFragment", "Registration failed: ${result.first}")
                    Snackbar.make(
                        view.findViewById(R.id.submitButton),
                        result.first,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }

        view.findViewById<Button>(R.id.submitButton).setOnClickListener {
            val username = view.findViewById<TextInputEditText>(R.id.editText1).text.toString()
            val email = view.findViewById<TextInputEditText>(R.id.editText2).text.toString()
            val password = view.findViewById<TextInputEditText>(R.id.editText3).text.toString()
            
            viewModel.registerUser(username, email, password)
        }
        
        val bottomNav = view.findViewById<BottomNavigationWidget>(R.id.bottomNavigationWidget)
        bottomNav.setActiveItem(BottomNavItem.LIST)
    }
}

