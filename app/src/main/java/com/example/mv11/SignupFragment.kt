package com.example.mv11

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
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
                // Hide keyboard before showing Snackbar
                hideKeyboard(view)
                
                result.second?.let { user ->
                    Log.d("SignupFragment", "Registration successful: $user")
                    Log.d("SignupFragment", "Access token: ${user.access}, length: ${user.access.length}")
                    Log.d("SignupFragment", "Refresh token: ${user.refresh}, length: ${user.refresh.length}")

                    PreferenceData.getInstance().putUser(context, user)
                    Log.d("SignupFragment", "User saved to SharedPreferences with access token")
                    
                    showSnackbar(view, getString(R.string.registration_success), Snackbar.LENGTH_SHORT)
                    findNavController().navigate(R.id.feedFragment)
                } ?: run {
                    Log.e("SignupFragment", "Registration failed: ${result.first}")
                    showSnackbar(view, result.first, Snackbar.LENGTH_LONG)
                }
            }
        }

        view.findViewById<Button>(R.id.submitButton).setOnClickListener {
            val username = view.findViewById<TextInputEditText>(R.id.editText1).text.toString()
            val email = view.findViewById<TextInputEditText>(R.id.editText2).text.toString()
            val password = view.findViewById<TextInputEditText>(R.id.editText3).text.toString()
            
            viewModel.registerUser(username, email, password)
        }

        view.findViewById<TextView>(R.id.tvLogin).setOnClickListener {
            findNavController().navigate(R.id.action_signup_to_prihlasenie)
        }
        
        val bottomNav = view.findViewById<BottomNavigationWidget>(R.id.bottomNavigationWidget)
        bottomNav.setActiveItem(BottomNavItem.LIST)
    }

    private fun hideKeyboard(view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showSnackbar(view: View, message: String, duration: Int) {
        val snackbar = Snackbar.make(
            view.findViewById(R.id.contentContainer),
            message,
            duration
        )
        // Set anchor to bottom navigation to show above it
        snackbar.anchorView = view.findViewById(R.id.bottomNavigationWidget)
        snackbar.show()
    }
}

