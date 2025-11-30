package com.example.mv11

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class PrihlasenieFragment : Fragment() {

    private lateinit var viewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_prihlasenie, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[AuthViewModel::class.java]

        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = view.findViewById<MaterialButton>(R.id.btnLogin)
        val tvSignup = view.findViewById<TextView>(R.id.tvSignup)
        val tvForgotPassword = view.findViewById<TextView>(R.id.tvForgotPassword)

        viewModel.loginResult.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { result ->
                // Hide keyboard before showing Snackbar
                hideKeyboard(view)
                
                result.second?.let { user ->
                    Log.d("PrihlasenieFragment", "Login successful: $user")
                    Log.d("PrihlasenieFragment", "Access token: ${user.access}, length: ${user.access.length}")
                    Log.d("PrihlasenieFragment", "Refresh token: ${user.refresh}, length: ${user.refresh.length}")

                    PreferenceData.getInstance().putUser(context, user)
                    Log.d("PrihlasenieFragment", "User saved to SharedPreferences")

                    showSnackbar(view, getString(R.string.toast_login_success), Snackbar.LENGTH_SHORT)
                    findNavController().navigate(R.id.feedFragment)
                } ?: run {
                    Log.e("PrihlasenieFragment", "Login failed: ${result.first}")
                    showSnackbar(view, result.first, Snackbar.LENGTH_LONG)
                }
            }
        }

        viewModel.passwordResetResult.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { result ->
                hideKeyboard(view)
                
                if (result.second) {
                    Log.d("PrihlasenieFragment", "Password reset email sent successfully")
                    showSnackbar(view, getString(R.string.password_reset_email_sent), Snackbar.LENGTH_LONG)
                } else {
                    Log.e("PrihlasenieFragment", "Password reset failed: ${result.first}")
                    showSnackbar(view, result.first, Snackbar.LENGTH_LONG)
                }
            }
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

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

        tvSignup.setOnClickListener {
            findNavController().navigate(R.id.action_prihlasenie_to_signup)
        }

        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            
            if (email.isEmpty()) {
                showSnackbar(view, getString(R.string.toast_enter_email_for_reset), Snackbar.LENGTH_SHORT)
                return@setOnClickListener
            }
            
            hideKeyboard(view)
            viewModel.resetPassword(email)
        }

        val bottomNav = view.findViewById<BottomNavigationWidget>(R.id.bottomNavigationWidget)
        bottomNav.setActiveItem(BottomNavItem.PROFILE)
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