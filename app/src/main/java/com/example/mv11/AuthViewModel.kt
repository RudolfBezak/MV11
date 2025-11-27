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
}

