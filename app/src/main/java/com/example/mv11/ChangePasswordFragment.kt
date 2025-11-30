package com.example.mv11

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class ChangePasswordFragment : Fragment() {

    private lateinit var viewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_change_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[AuthViewModel::class.java]

        val etOldPassword = view.findViewById<TextInputEditText>(R.id.etOldPassword)
        val etNewPassword = view.findViewById<TextInputEditText>(R.id.etNewPassword)
        val btnChangePassword = view.findViewById<MaterialButton>(R.id.btnChangePassword)
        val tvForgotPassword = view.findViewById<View>(R.id.tvForgotPassword)

        viewModel.passwordChangeResult.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { result ->
                hideKeyboard(view)
                
                if (result.second) {
                    Log.d("ChangePasswordFragment", "Password changed successfully")
                    showSnackbar(view, getString(R.string.password_change_success), Snackbar.LENGTH_SHORT)
                    findNavController().popBackStack()
                } else {
                    Log.e("ChangePasswordFragment", "Password change failed: ${result.first}")
                    showSnackbar(view, result.first, Snackbar.LENGTH_LONG)
                }
            }
        }

        viewModel.passwordResetResult.observe(viewLifecycleOwner) { evento ->
            evento.getContentIfNotHandled()?.let { result ->
                hideKeyboard(view)
                
                if (result.second) {
                    Log.d("ChangePasswordFragment", "Password reset email sent successfully")
                    showSnackbar(view, getString(R.string.password_reset_email_sent), Snackbar.LENGTH_LONG)
                } else {
                    Log.e("ChangePasswordFragment", "Password reset failed: ${result.first}")
                    showSnackbar(view, result.first, Snackbar.LENGTH_LONG)
                }
            }
        }

        btnChangePassword.setOnClickListener {
            val oldPassword = etOldPassword.text.toString().trim()
            val newPassword = etNewPassword.text.toString().trim()

            if (oldPassword.isEmpty()) {
                showSnackbar(view, getString(R.string.toast_enter_old_password), Snackbar.LENGTH_SHORT)
                return@setOnClickListener
            }

            if (newPassword.isEmpty()) {
                showSnackbar(view, getString(R.string.toast_enter_new_password), Snackbar.LENGTH_SHORT)
                return@setOnClickListener
            }

            val user = PreferenceData.getInstance().getUser(context)
            if (user != null && user.access.isNotEmpty()) {
                viewModel.changePassword(user.access, oldPassword, newPassword)
            } else {
                showSnackbar(view, getString(R.string.no_user_logged_in), Snackbar.LENGTH_SHORT)
            }
        }

        tvForgotPassword.setOnClickListener {
            val user = PreferenceData.getInstance().getUser(context)
            if (user != null && user.email.isNotEmpty()) {
                viewModel.resetPassword(user.email)
            } else {
                showSnackbar(view, getString(R.string.error_email_not_found), Snackbar.LENGTH_SHORT)
            }
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
        snackbar.anchorView = view.findViewById(R.id.bottomNavigationWidget)
        snackbar.show()
    }
}

