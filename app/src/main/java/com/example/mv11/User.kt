package com.example.mv11

import com.google.gson.Gson
import java.io.IOException

/**
 * User - dátová trieda reprezentujúca používateľa aplikácie.
 * 
 * Obsahuje:
 * - name - meno používateľa
 * - email - email používateľa
 * - uid - jedinečný identifikátor používateľa
 * - access - access token pre API autentifikáciu
 * - refresh - refresh token pre obnovenie access tokenu
 */
data class User(
    val name: String,
    val email: String,
    val uid: String,
    val access: String,
    val refresh: String,
    val photo: String = ""
) {
    /**
     * Konvertuje User objekt na JSON string.
     * Používa sa pri ukladaní do SharedPreferences.
     * 
     * @return JSON string reprezentácia User objektu, alebo null pri chybe
     */
    fun toJson(): String? {
        return try {
            Gson().toJson(this)
        } catch (ex: IOException) {
            ex.printStackTrace()
            null
        }
    }

    companion object {
        /**
         * Vytvorí User objekt z JSON stringu.
         * Používa sa pri načítaní z SharedPreferences.
         * 
         * @param string - JSON string reprezentácia User objektu
         * @return User objekt, alebo null pri chybe parsovania
         */
        fun fromJson(string: String): User? {
            return try {
                Gson().fromJson(string, User::class.java)
            } catch (ex: IOException) {
                ex.printStackTrace()
                null
            }
        }
    }
}

data class UserRegistration(
    val name: String,
    val email: String,
    val password: String
)

data class RegistrationResponse(
    val uid: String,
    val access: String,
    val refresh: String
)

data class UserResponse(
    val uid: String,
    val name: String,
    val updated: String,
    val lat: Double,
    val lon: Double,
    val radius: Double,
    val photo: String = ""
)

/**
 * RefreshTokenRequest - request body pre obnovenie access tokenu.
 * 
 * @param refresh - refresh token získaný pri registrácii/prihlásení
 */
data class RefreshTokenRequest(
    val refresh: String
)

/**
 * RefreshTokenResponse - odpoveď z API pri obnovení tokenu.
 * 
 * @param access - nový access token
 * @param refresh - nový refresh token (môže byť rovnaký alebo nový)
 */
data class RefreshTokenResponse(
    val access: String,
    val refresh: String
)

/**
 * GeofenceUpdateRequest - request body pre odoslanie polohy.
 * 
 * @param lat - zemepisná šírka (latitude)
 * @param lon - zemepisná dĺžka (longitude)
 * @param radius - polomer geofence oblasti v metroch
 */
data class GeofenceUpdateRequest(
    val lat: Double,
    val lon: Double,
    val radius: Double
)

/**
 * GeofenceResponse - odpoveď z geofence API.
 * 
 * @param uid - uid používateľa
 * @param name - meno používateľa
 * @param lat - zemepisná šírka
 * @param lon - zemepisná dĺžka
 * @param radius - polomer geofence
 * @param updated - dátum a čas aktualizácie
 */
data class GeofenceResponse(
    val uid: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val radius: Double,
    val updated: String
)

