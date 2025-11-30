package com.example.mv11

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class IntroFragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_intro, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        updateUI(view)
        
        val bottomNav = view.findViewById<BottomNavigationWidget>(R.id.bottomNavigationWidget)
        bottomNav.setActiveItem(BottomNavItem.MAP)
    }

    override fun onResume() {
        super.onResume()
        view?.let { updateUI(it) }
    }

    private fun updateUI(view: View) {
        val user = PreferenceData.getInstance().getUser(context)
        val button1 = view.findViewById<Button>(R.id.button1) // Login
        val button2 = view.findViewById<Button>(R.id.button2) // Signup
        val buttonMap = view.findViewById<Button>(R.id.buttonMap) // Map
        val buttonUsers = view.findViewById<Button>(R.id.buttonUsers) // Users
        val buttonProfile = view.findViewById<Button>(R.id.buttonProfile) // Profile

        if (user != null) {
            // User is logged in - show Map, Users, Profile buttons
            button1.visibility = View.GONE
            button2.visibility = View.GONE
            buttonMap.visibility = View.VISIBLE
            buttonUsers.visibility = View.VISIBLE
            buttonProfile.visibility = View.VISIBLE

            buttonMap.setOnClickListener {
                findNavController().navigate(R.id.mapFragment)
            }

            buttonUsers.setOnClickListener {
                findNavController().navigate(R.id.feedFragment)
            }

            buttonProfile.setOnClickListener {
                findNavController().navigate(R.id.profileFragment)
            }
        } else {
            // User is not logged in - show Login, Signup buttons
            button1.visibility = View.VISIBLE
            button2.visibility = View.VISIBLE
            buttonMap.visibility = View.GONE
            buttonUsers.visibility = View.GONE
            buttonProfile.visibility = View.GONE

            button1.setOnClickListener {
                findNavController().navigate(R.id.action_intro_to_prihlasenie)
            }

            button2.setOnClickListener {
                findNavController().navigate(R.id.action_intro_to_signup)
            }
        }
    }
}

