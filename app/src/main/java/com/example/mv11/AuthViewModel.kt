package com.example.mv11

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class AuthViewModel(private val dataRepository: DataRepository) : ViewModel() {
    private val _registrationResult = MutableLiveData<Evento<Pair<String, User?>>>()
    val registrationResult: LiveData<Evento<Pair<String, User?>>> get() = _registrationResult

    private val _loginResult = MutableLiveData<Evento<Pair<String, User?>>>()
    val loginResult: LiveData<Evento<Pair<String, User?>>> get() = _loginResult

    private val _logoutResult = MutableLiveData<Evento<Pair<String, Boolean>>>()
    val logoutResult: LiveData<Evento<Pair<String, Boolean>>> get() = _logoutResult

    private val _passwordResetResult = MutableLiveData<Evento<Pair<String, Boolean>>>()
    val passwordResetResult: LiveData<Evento<Pair<String, Boolean>>> get() = _passwordResetResult

    private val _passwordChangeResult = MutableLiveData<Evento<Pair<String, Boolean>>>()
    val passwordChangeResult: LiveData<Evento<Pair<String, Boolean>>> get() = _passwordChangeResult

    private val _geofenceUpdateResult = MutableLiveData<Evento<Pair<String, Boolean>>>()
    val geofenceUpdateResult: LiveData<Evento<Pair<String, Boolean>>> get() = _geofenceUpdateResult

    private val _geofenceDeleteResult = MutableLiveData<Evento<Pair<String, Boolean>>>()
    val geofenceDeleteResult: LiveData<Evento<Pair<String, Boolean>>> get() = _geofenceDeleteResult

    fun registerUser(username: String, email: String, password: String) {
        viewModelScope.launch {
            val result = dataRepository.apiRegisterUser(username, email, password)
            _registrationResult.postValue(Evento(result))
        }
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            val result = dataRepository.apiLoginUser(email, password)
            _loginResult.postValue(Evento(result))
        }
    }

    fun logout(accessToken: String) {
        viewModelScope.launch {
            val result = dataRepository.apiLogout(accessToken)
            _logoutResult.postValue(Evento(result))
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            val result = dataRepository.apiResetPassword(email)
            _passwordResetResult.postValue(Evento(result))
        }
    }

    fun changePassword(accessToken: String, oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            val result = dataRepository.apiChangePassword(accessToken, oldPassword, newPassword)
            _passwordChangeResult.postValue(Evento(result))
        }
    }

    fun updateGeofence(accessToken: String, lat: Double, lon: Double, radius: Double) {
        viewModelScope.launch {
            val result = dataRepository.apiUpdateGeofence(accessToken, lat, lon, radius)
            _geofenceUpdateResult.postValue(Evento(result))
        }
    }

    fun deleteGeofence(accessToken: String) {
        viewModelScope.launch {
            val result = dataRepository.apiDeleteGeofence(accessToken)
            _geofenceDeleteResult.postValue(Evento(result))
        }
    }
}

