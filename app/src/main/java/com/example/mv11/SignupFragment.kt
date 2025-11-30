package com.example.mv11

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.mv11.databinding.FragmentSignupBinding
import com.google.android.material.snackbar.Snackbar

class SignupFragment : Fragment(R.layout.fragment_signup) {

    private var binding: FragmentSignupBinding? = null
    private lateinit var viewModel: AuthViewModel
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[AuthViewModel::class.java]

        binding = FragmentSignupBinding.bind(view).apply {
            lifecycleOwner = viewLifecycleOwner
            this.viewModel = this@SignupFragment.viewModel
        }

        binding?.let { bnd ->
            viewModel.registrationResult.observe(viewLifecycleOwner) { evento ->
                evento.getContentIfNotHandled()?.let { result ->
                    hideKeyboard(bnd.root)
                    
                    result.second?.let { user ->
                        Log.d("SignupFragment", "Registration successful: $user")
                        Log.d("SignupFragment", "Access token: ${user.access}, length: ${user.access.length}")
                        Log.d("SignupFragment", "Refresh token: ${user.refresh}, length: ${user.refresh.length}")

                        PreferenceData.getInstance().putUser(context, user)
                        Log.d("SignupFragment", "User saved to SharedPreferences with access token")
                        
                        showSnackbar(bnd.root, getString(R.string.registration_success), Snackbar.LENGTH_SHORT)
                        findNavController().navigate(R.id.feedFragment)
                    } ?: run {
                        Log.e("SignupFragment", "Registration failed: ${result.first}")
                        showSnackbar(bnd.root, result.first, Snackbar.LENGTH_LONG)
                    }
                }
            }

            bnd.submitButton.setOnClickListener {
                val username = viewModel.signupUsername.get()?.trim() ?: ""
                val email = viewModel.signupEmail.get()?.trim() ?: ""
                val password = viewModel.signupPassword.get()?.trim() ?: ""
                
                if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    showSnackbar(bnd.root, "Prosím vyplňte všetky polia", Snackbar.LENGTH_SHORT)
                    return@setOnClickListener
                }
                
                viewModel.registerUser()
            }

            bnd.tvLogin.setOnClickListener {
                findNavController().navigate(R.id.action_signup_to_prihlasenie)
            }
            
            bnd.bottomNavigationWidget.setActiveItem(BottomNavItem.LIST)
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun hideKeyboard(view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showSnackbar(view: View, message: String, duration: Int) {
        binding?.let { bnd ->
            val snackbar = Snackbar.make(
                bnd.contentContainer,
                message,
                duration
            )
            snackbar.anchorView = bnd.bottomNavigationWidget
            snackbar.show()
        }
    }
}

