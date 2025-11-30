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
    val refresh: String
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

data class UserLogin(
    val name: String,  // Can be username or email
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

data class PasswordResetRequest(
    val email: String
)

data class PasswordResetResponse(
    val status: String,
    val message: String? = null
)

data class PasswordChangeRequest(
    val old_password: String,
    val new_password: String
)

data class PasswordChangeResponse(
    val status: String
)

data class GeofenceUpdateRequest(
    val lat: Double,
    val lon: Double,
    val radius: Double
)

data class GeofenceUpdateResponse(
    val success: Boolean
)

data class GeofenceMe(
    val uid: String,
    val lat: String,
    val lon: String,
    val radius: String
)

data class GeofenceUserItem(
    val uid: String,
    val radius: String,
    val updated: String,
    val name: String,
    val photo: String
)

data class GeofenceListResponse(
    val me: GeofenceMe? = null,
    val list: List<GeofenceUserItem>
)

data class UserProfileResponse(
    val id: String,
    val name: String,
    val photo: String
)

data class PhotoUploadResponse(
    val id: String,
    val name: String,
    val photo: String
)

data class RefreshTokenRequest(
    val refresh: String
)

data class RefreshTokenResponse(
    val uid: String,
    val access: String,
    val refresh: String
)

