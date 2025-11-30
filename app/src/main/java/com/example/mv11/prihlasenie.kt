package com.example.mv11

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.mv11.databinding.FragmentPrihlasenieBinding
import com.google.android.material.snackbar.Snackbar

class PrihlasenieFragment : Fragment(R.layout.fragment_prihlasenie) {

    private var binding: FragmentPrihlasenieBinding? = null
    private lateinit var viewModel: AuthViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[AuthViewModel::class.java]

        binding = FragmentPrihlasenieBinding.bind(view).apply {
            lifecycleOwner = viewLifecycleOwner
            this.viewModel = this@PrihlasenieFragment.viewModel
        }

        binding?.let { bnd ->
            viewModel.loginResult.observe(viewLifecycleOwner) { evento ->
                evento.getContentIfNotHandled()?.let { result ->
                    hideKeyboard(bnd.root)
                    
                    result.second?.let { user ->
                        Log.d("PrihlasenieFragment", "Login successful: $user")
                        Log.d("PrihlasenieFragment", "Access token: ${user.access}, length: ${user.access.length}")
                        Log.d("PrihlasenieFragment", "Refresh token: ${user.refresh}, length: ${user.refresh.length}")

                        PreferenceData.getInstance().putUser(context, user)
                        Log.d("PrihlasenieFragment", "User saved to SharedPreferences")

                        showSnackbar(bnd.root, getString(R.string.toast_login_success), Snackbar.LENGTH_SHORT)
                        findNavController().navigate(R.id.feedFragment)
                    } ?: run {
                        Log.e("PrihlasenieFragment", "Login failed: ${result.first}")
                        showSnackbar(bnd.root, result.first, Snackbar.LENGTH_LONG)
                    }
                }
            }

            viewModel.passwordResetResult.observe(viewLifecycleOwner) { evento ->
                evento.getContentIfNotHandled()?.let { result ->
                    hideKeyboard(bnd.root)
                    
                    if (result.second) {
                        Log.d("PrihlasenieFragment", "Password reset email sent successfully")
                        showSnackbar(bnd.root, getString(R.string.password_reset_email_sent), Snackbar.LENGTH_LONG)
                    } else {
                        Log.e("PrihlasenieFragment", "Password reset failed: ${result.first}")
                        showSnackbar(bnd.root, result.first, Snackbar.LENGTH_LONG)
                    }
                }
            }

            bnd.btnLogin.setOnClickListener {
                val email = bnd.etEmail.text.toString().trim()
                val password = bnd.etPassword.text.toString().trim()

                if (email.isEmpty()) {
                    Toast.makeText(context, getString(R.string.toast_enter_email), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (password.isEmpty()) {
                    Toast.makeText(context, getString(R.string.toast_enter_password), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                viewModel.loginUser(email, password)
            }

            bnd.tvSignup.setOnClickListener {
                findNavController().navigate(R.id.action_prihlasenie_to_signup)
            }

            bnd.tvForgotPassword.setOnClickListener {
                val email = bnd.etEmail.text.toString().trim()
                
                if (email.isEmpty()) {
                    showSnackbar(bnd.root, getString(R.string.toast_enter_email_for_reset), Snackbar.LENGTH_SHORT)
                    return@setOnClickListener
                }
                
                hideKeyboard(bnd.root)
                viewModel.resetPassword(email)
            }

            bnd.bottomNavigationWidget.setActiveItem(BottomNavItem.PROFILE)
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