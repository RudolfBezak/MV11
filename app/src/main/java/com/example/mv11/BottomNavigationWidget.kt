package com.example.mv11

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.findNavController

class BottomNavigationWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val btnNavMap: ImageView
    private val btnNavList: ImageView
    private val btnNavProfile: ImageView
    
    private var currentActiveItem: BottomNavItem = BottomNavItem.MAP

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_bottom_navigation, this, true)
        
        btnNavMap = findViewById(R.id.btnNavMap)
        btnNavList = findViewById(R.id.btnNavList)
        btnNavProfile = findViewById(R.id.btnNavProfile)
        
        setupClickListeners()
        setupWindowInsets()
    }
    
    private fun setupWindowInsets() {
        setOnApplyWindowInsetsListener { view, insets ->
            val systemBarsInsets = insets.getInsets(
                android.view.WindowInsets.Type.systemBars()
            )
            view.setPadding(
                paddingLeft,
                paddingTop,
                paddingRight,
                systemBarsInsets.bottom
            )
            insets
        }
    }
    
    private fun setupClickListeners() {
        btnNavMap.setOnClickListener {
            navigateToMap()
        }
        
        btnNavList.setOnClickListener {
            navigateToFeed()
        }
        
        btnNavProfile.setOnClickListener {
            navigateToProfile()
        }
    }
    
    private fun navigateToMap() {
        try {
            val navController = findNavController()
            if (navController.currentDestination?.id != R.id.mapFragment) {
                val navOptions = androidx.navigation.NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .build()
                navController.navigate(R.id.mapFragment, null, navOptions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun navigateToFeed() {
        try {
            val navController = findNavController()
            if (navController.currentDestination?.id != R.id.feedFragment) {
                val navOptions = androidx.navigation.NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .build()
                navController.navigate(R.id.feedFragment, null, navOptions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun navigateToProfile() {
        try {
            val navController = findNavController()
            if (navController.currentDestination?.id != R.id.profileFragment) {
                val navOptions = androidx.navigation.NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .build()
                navController.navigate(R.id.profileFragment, null, navOptions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun setActiveItem(item: BottomNavItem) {
        currentActiveItem = item
        updateHighlight()
    }
    
    private fun updateHighlight() {
        val activeColor = context.getColor(R.color.secondary_yellow)
        val inactiveColor = context.getColor(R.color.white)
        
        btnNavMap.setColorFilter(
            if (currentActiveItem == BottomNavItem.MAP) activeColor else inactiveColor
        )
        btnNavList.setColorFilter(
            if (currentActiveItem == BottomNavItem.LIST) activeColor else inactiveColor
        )
        btnNavProfile.setColorFilter(
            if (currentActiveItem == BottomNavItem.PROFILE) activeColor else inactiveColor
        )
    }
}

enum class BottomNavItem {
    MAP, LIST, PROFILE
}

