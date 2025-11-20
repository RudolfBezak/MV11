package com.example.mv11

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar

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

        viewModel.registrationResult.observe(viewLifecycleOwner) {
            if (it.second != null) {
                val user = it.second
                Log.d("SignupFragment", "Registration successful: $user")
                
                // Uložiť používateľa do SharedPreferences
                PreferenceData.getInstance().putUser(context, user)
                Log.d("SignupFragment", "User saved to SharedPreferences")
                
                Snackbar.make(
                    view.findViewById(R.id.submitButton),
                    "Registration successful!",
                    Snackbar.LENGTH_SHORT
                ).show()
                findNavController().navigate(R.id.feedFragment)
            } else {
                Log.e("SignupFragment", "Registration failed: ${it.first}")
                Snackbar.make(
                    view.findViewById(R.id.submitButton),
                    it.first,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        view.findViewById<Button>(R.id.submitButton).setOnClickListener {
            val username = view.findViewById<EditText>(R.id.editText1).text.toString()
            val email = view.findViewById<EditText>(R.id.editText2).text.toString()
            val password = view.findViewById<EditText>(R.id.editText3).text.toString()
            
            viewModel.registerUser(username, email, password)
        }
        
        val bottomNav = view.findViewById<BottomNavigationWidget>(R.id.bottomNavigationWidget)
        bottomNav.setActiveItem(BottomNavItem.LIST)
    }
}

