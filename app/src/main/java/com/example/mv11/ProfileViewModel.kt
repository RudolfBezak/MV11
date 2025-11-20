package com.example.mv11

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * ProfileViewModel - ViewModel pre ProfileFragment.
 * 
 * Spravuje:
 * - Stav zdieľania polohy (sharingLocation)
 * - Komunikáciu s DataRepository
 */
class ProfileViewModel(private val dataRepository: DataRepository) : ViewModel() {

    /**
     * Stav zdieľania polohy.
     * null = ešte nebol načítaný z SharedPreferences
     * true = používateľ zdieľa polohu
     * false = používateľ nezdieľa polohu
     */
    val sharingLocation = MutableLiveData<Boolean?>(null)

    /**
     * Správa pre používateľa (error alebo success).
     */
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> get() = _message

    /**
     * Aktualizuje stav zdieľania polohy.
     * 
     * @param sharing - true ak používateľ chce zdieľať polohu, false ak nie
     */
    fun updateSharingLocation(sharing: Boolean) {
        sharingLocation.value = sharing
    }
}

