package com.example.mv11

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
                result.second?.let { user ->
                    Log.d("PrihlasenieFragment", "Login successful: $user")
                    Log.d("PrihlasenieFragment", "Access token: ${user.access}, length: ${user.access.length}")
                    Log.d("PrihlasenieFragment", "Refresh token: ${user.refresh}, length: ${user.refresh.length}")

                    PreferenceData.getInstance().putUser(context, user)
                    Log.d("PrihlasenieFragment", "User saved to SharedPreferences")

                    Snackbar.make(
                        btnLogin,
                        getString(R.string.toast_login_success),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    findNavController().navigate(R.id.feedFragment)
                } ?: run {
                    Log.e("PrihlasenieFragment", "Login failed: ${result.first}")
                    Snackbar.make(
                        btnLogin,
                        result.first,
                        Snackbar.LENGTH_SHORT
                    ).show()
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
            Toast.makeText(context, getString(R.string.toast_forgot_not_implemented), Toast.LENGTH_SHORT).show()
        }

        val bottomNav = view.findViewById<BottomNavigationWidget>(R.id.bottomNavigationWidget)
        bottomNav.setActiveItem(BottomNavItem.PROFILE)
    }
}