package com.example.mv11

import android.app.Application
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map

class IntroViewModel(application: Application) : AndroidViewModel(application) {

    private val _isUserLoggedIn = MutableLiveData<Boolean>()
    val isUserLoggedIn: LiveData<Boolean> get() = _isUserLoggedIn

    val loginButtonsVisibility: LiveData<Int> = _isUserLoggedIn.map { isLoggedIn ->
        if (isLoggedIn) View.GONE else View.VISIBLE
    }

    val loggedInButtonsVisibility: LiveData<Int> = _isUserLoggedIn.map { isLoggedIn ->
        if (isLoggedIn) View.VISIBLE else View.GONE
    }

    init {
        checkUserLoginStatus()
    }

    fun checkUserLoginStatus() {
        val user = PreferenceData.getInstance().getUser(getApplication())
        _isUserLoggedIn.value = user != null
    }

    fun refreshUserStatus() {
        checkUserLoginStatus()
    }
}

