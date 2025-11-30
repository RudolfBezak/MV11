package com.example.mv11

import android.content.Context
import android.content.SharedPreferences

/**
 * PreferenceData - singleton trieda pre prácu so SharedPreferences.
 * 
 * SharedPreferences je mechanizmus na ukladanie jednoduchých párov kľúč-hodnota.
 * Vhodné pre:
 * - Nastavenia používateľa
 * - Konfigurácie aplikácie
 * - Uloženie používateľskej relácie (session)
 * 
 * Singleton pattern zabezpečuje že existuje iba jedna inštancia v celej aplikácii.
 */
class PreferenceData private constructor() {

    /**
     * Získa inštanciu SharedPreferences.
     * 
     * @param context - kontext aplikácie (Activity, Fragment, Application)
     * @return SharedPreferences inštancia, alebo null ak context je null
     */
    private fun getSharedPreferences(context: Context?): SharedPreferences? {
        return context?.getSharedPreferences(
            shpKey, Context.MODE_PRIVATE
        )
    }

    /**
     * Vymaže všetky dáta z SharedPreferences.
     * Používa sa pri odhlásení používateľa.
     * 
     * @param context - kontext aplikácie
     */
    fun clearData(context: Context?) {
        val sharedPref = getSharedPreferences(context) ?: return
        val editor = sharedPref.edit()
        editor.clear()
        editor.apply()  // Asynchrónne uloženie (lepšie ako commit())
    }

    /**
     * Uloží User objekt do SharedPreferences.
     * 
     * @param context - kontext aplikácie
     * @param user - User objekt na uloženie, alebo null pre vymazanie
     */
    fun putUser(context: Context?, user: User?) {
        val sharedPref = getSharedPreferences(context) ?: return
        val editor = sharedPref.edit()

        if (user != null) {
            // Konvertuj User na JSON a ulož
            user.toJson()?.let {
                editor.putString(userKey, it)
            } ?: editor.remove(userKey)  // Ak konverzia zlyhá, vymaž kľúč
        } else {
            // Ak je user null, vymaž uloženého používateľa
            editor.remove(userKey)
        }

        editor.apply()  // Asynchrónne uloženie
    }

    /**
     * Načíta User objekt z SharedPreferences.
     * 
     * @param context - kontext aplikácie
     * @return User objekt ak existuje, inak null
     */
    fun getUser(context: Context?): User? {
        val sharedPref = getSharedPreferences(context) ?: return null
        val json = sharedPref.getString(userKey, null) ?: return null

        // Konvertuj JSON späť na User objekt
        return User.fromJson(json)
    }

    /**
     * Uloží stav zdieľania polohy do SharedPreferences.
     * 
     * @param context - kontext aplikácie
     * @param enabled - true ak je zdieľanie polohy zapnuté, false ak je vypnuté
     */
    fun setLocationSharingEnabled(context: Context?, enabled: Boolean) {
        val sharedPref = getSharedPreferences(context) ?: return
        val editor = sharedPref.edit()
        editor.putBoolean(locationSharingKey, enabled)
        editor.apply()
    }

    /**
     * Načíta stav zdieľania polohy z SharedPreferences.
     * 
     * @param context - kontext aplikácie
     * @return true ak je zdieľanie polohy zapnuté, false ak je vypnuté (default: false)
     */
    fun getLocationSharingEnabled(context: Context?): Boolean {
        val sharedPref = getSharedPreferences(context) ?: return false
        return sharedPref.getBoolean(locationSharingKey, false)
    }

    companion object {
        /**
         * Singleton inštancia PreferenceData.
         * 
         * @Volatile - zaručuje viditeľnosť zmien naprieč vláknami
         */
        @Volatile
        private var INSTANCE: PreferenceData? = null

        /**
         * Lock objekt pre thread-safe vytvorenie singletonu.
         */
        private val lock = Any()

        /**
         * Vracia jedinú inštanciu PreferenceData.
         * 
         * synchronized(lock) - zabezpečuje že iba jedno vlákno môže vytvárať
         * inštanciu naraz (thread-safe singleton pattern).
         */
        fun getInstance(): PreferenceData =
            INSTANCE ?: synchronized(lock) {
                INSTANCE ?: PreferenceData().also { INSTANCE = it }
            }

        /**
         * Názov SharedPreferences súboru.
         * Dáta sa uložia do: /data/data/com.example.mv11/shared_prefs/eu.mcomputing.mobv.zadanie.xml
         */
        private const val shpKey = "eu.mcomputing.mobv.zadanie"

        /**
         * Kľúč pre uloženie User objektu v SharedPreferences.
         */
        private const val userKey = "userKey"

        /**
         * Kľúč pre uloženie stavu zdieľania polohy v SharedPreferences.
         */
        private const val locationSharingKey = "locationSharingEnabled"
    }
}

