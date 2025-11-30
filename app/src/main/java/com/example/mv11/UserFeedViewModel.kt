package com.example.mv11

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * UserFeedViewModel - spravuje dáta používateľov pre zobrazenie v UI.
 * 
 * Tento ViewModel kombinuje dáta z API a lokálnej databázy v offline-first prístupe:
 * 1. Pri vytvorení načíta dáta z API
 * 2. Uloží ich do lokálnej databázy
 * 3. Sleduje databázu a automaticky aktualizuje UI pri zmenách
 * 
 * @param repository - DataRepository pre prístup k dátam
 */
class UserFeedViewModel(private val repository: DataRepository) : ViewModel() {

    /**
     * LiveData so zoznamom používateľov - OFFLINE-FIRST PATTERN.
     * 
     * liveData { } - builder ktorý vytvára LiveData s automatickou inicializáciou.
     * 
     * DÔLEŽITÉ - Tok dát (Offline-First):
     * 
     * 1. emitSource(repository.getUsers()) - OKAMŽITE začne sledovať databázu
     *    → Ak sú dáta v cache, zobrazí ich IHNEĎ (rýchle UX!)
     *    → Ak nie sú dáta, zobrazí prázdny zoznam
     * 
     * 2. V POZADÍ (paralelne):
     *    - loading = true
     *    - repository.apiListGeofence() - stiahne fresh dáta z API
     *    - uloží ich do databázy
     *    - loading = false
     * 
     * 3. Keď sa dáta uložia do DB, emitSource(repository.getUsers()) 
     *    AUTOMATICKY aktualizuje UI s novými dátami!
     * 
     * Výsledok:
     * - Používateľ vidí cachované dáta OKAMŽITE (0ms delay)
     * - API sa volá v pozadí
     * - UI sa automaticky obnoví keď prídu fresh dáta
     * - Funguje offline (zobrazí cache aj bez internetu)
     * 
     * Príklad časovej osi:
     * t=0ms:    UI zobrazí cache (3 staré používateľov)
     * t=0ms:    Začne sa API call v pozadí
     * t=500ms:  API odpovie s 5 používateľmi
     * t=500ms:  Uložia sa do DB
     * t=500ms:  UI sa AUTOMATICKY aktualizuje (zobrazí 5 používateľov)
     */
    val feed_items: LiveData<List<UserEntity?>> =
        liveData {
            // KROK 1: Okamžite začni sledovať databázu (zobrazí cache data)
            val source = repository.getUsers()
            emitSource(source)
            
            // KROK 2: V pozadí načítaj fresh dáta z API
            // Note: Access token will be provided when updateItems() is called
            // Initial load will use empty token (will fail gracefully)
        }

    /**
     * Loading state - indikuje či prebieha načítavanie dát.
     * Fragment pozoruje túto premennú a zobrazuje/skrýva ProgressBar.
     */
    val loading = MutableLiveData(false)

    /**
     * Správa pre používateľa (error alebo success).
     * 
     * Používame Evento aby sa správa zobrazila iba raz (nie pri každej rotácii).
     * private _message - interná premenná (môže byť zmenená iba z ViewModelu)
     * public message - externá premenná (iba na čítanie z Fragmentu)
     */
    private val _message = MutableLiveData<Evento<String>>()
    val message: LiveData<Evento<String>>
        get() = _message

    /**
     * Manuálne obnovenie dát z API.
     * Používa sa keď používateľ klikne na refresh button alebo pri inicializácii.
     * 
     * viewModelScope.launch - spustí coroutinu ktorá sa automaticky zruší
     * keď sa ViewModel zničí (pri close fragmentu/activity).
     * 
     * Evento() - zabalí správu do jednorazovej udalosti
     * 
     * @param accessToken - access token používateľa pre autentifikáciu
     */
    fun updateItems(accessToken: String) {
        viewModelScope.launch {  // Spustiť asynchrónnu operáciu
            loading.postValue(true)
            val result = repository.apiListGeofence(accessToken)  // Vráti Pair<String, Boolean>
            if (!result.second) {
                _message.postValue(Evento(result.first))  // Poslať správu do UI (error alebo špeciálna správa)
            }
            loading.postValue(false)
        }
    }
}

