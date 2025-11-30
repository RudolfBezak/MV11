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
import com.example.mv11.databinding.FragmentChangePasswordBinding
import com.google.android.material.snackbar.Snackbar

class ChangePasswordFragment : Fragment(R.layout.fragment_change_password) {

    private var binding: FragmentChangePasswordBinding? = null
    private lateinit var viewModel: AuthViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[AuthViewModel::class.java]

        binding = FragmentChangePasswordBinding.bind(view).apply {
            lifecycleOwner = viewLifecycleOwner
            this.viewModel = this@ChangePasswordFragment.viewModel
        }

        binding?.let { bnd ->
            viewModel.passwordChangeResult.observe(viewLifecycleOwner) { evento ->
                evento.getContentIfNotHandled()?.let { result ->
                    hideKeyboard(bnd.root)
                    
                    if (result.second) {
                        Log.d("ChangePasswordFragment", "Password changed successfully")
                        showSnackbar(bnd.root, getString(R.string.password_change_success), Snackbar.LENGTH_SHORT)
                        findNavController().popBackStack()
                    } else {
                        Log.e("ChangePasswordFragment", "Password change failed: ${result.first}")
                        showSnackbar(bnd.root, result.first, Snackbar.LENGTH_LONG)
                    }
                }
            }

            bnd.btnChangePassword.setOnClickListener {
                val oldPwd = viewModel.oldPassword.get()?.trim() ?: ""
                val newPwd = viewModel.newPassword.get()?.trim() ?: ""

                if (oldPwd.isEmpty()) {
                    showSnackbar(bnd.root, getString(R.string.toast_enter_old_password), Snackbar.LENGTH_SHORT)
                    return@setOnClickListener
                }

                if (newPwd.isEmpty()) {
                    showSnackbar(bnd.root, getString(R.string.toast_enter_new_password), Snackbar.LENGTH_SHORT)
                    return@setOnClickListener
                }

                val user = PreferenceData.getInstance().getUser(context)
                if (user != null && user.access.isNotEmpty()) {
                    viewModel.changePassword(user.access)
                } else {
                    showSnackbar(bnd.root, getString(R.string.no_user_logged_in), Snackbar.LENGTH_SHORT)
                }
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

