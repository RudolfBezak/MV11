package com.example.mv11

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class PrihlasenieFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_prihlasenie, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = view.findViewById<MaterialButton>(R.id.btnLogin)
        val tvSignup = view.findViewById<TextView>(R.id.tvSignup)
        val tvForgotPassword = view.findViewById<TextView>(R.id.tvForgotPassword)
        
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
            
            Toast.makeText(context, getString(R.string.toast_login_success), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
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