package com.example.mv11

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NumberViewModel : ViewModel() {
    private val _randomNumber = MutableLiveData<Int>()
    val randomNumber: LiveData<Int> get() = _randomNumber

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    init {
        _randomNumber.value = 0
        _isLoading.value = false
    }

    fun generateRandomNumber() {
        viewModelScope.launch {
            _isLoading.value = true
            val number = fetchRandomNumber()
            _randomNumber.postValue(number)
            _isLoading.value = false
        }
    }

    private suspend fun fetchRandomNumber(): Int {
        delay(5000)
        return (0..10).random()
    }
}

