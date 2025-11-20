package com.example.mv11

import android.content.Context
import android.util.Log
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Route
import retrofit2.Call
import retrofit2.Response

/**
 * TokenAuthenticator - automaticky obnovuje access token pri 401 chybe.
 * 
 * Účel:
 * - Detekuje 401 Unauthorized odpoveď
 * - Automaticky obnoví access token pomocou refresh tokenu
 * - Retry pôvodnú požiadavku s novým tokenom
 * - Ak refresh zlyhá, vymaže používateľské dáta (logout)
 * 
 * Ako to funguje:
 * 1. Server vráti 401 (token expiroval)
 * 2. TokenAuthenticator sa automaticky zavolá
 * 3. Získa refresh token z SharedPreferences
 * 4. Zavolá refreshTokenBlocking() synchronne
 * 5. Uloží nové tokeny do SharedPreferences
 * 6. Vráti nový Request s novým tokenom
 * 7. OkHttp automaticky retry pôvodnú požiadavku
 * 
 * @param context - kontext aplikácie pre prístup k SharedPreferences
 */
class TokenAuthenticator(private val context: Context) : Authenticator {

    companion object {
        private const val TAG = "TokenAuthenticator"
    }

    /**
     * Automaticky obnoví token pri 401 chybe.
     * 
     * @param route - route požiadavky (môže byť null)
     * @param response - HTTP odpoveď s 401 chybou
     * @return nový Request s obnoveným tokenom, alebo null ak refresh zlyhal
     */
    override fun authenticate(route: Route?, response: okhttp3.Response): Request? {
        // KROK 1: Skontroluj či je to 401 chyba
        if (response.code != 401) {
            Log.d(TAG, "Response code is not 401 (${response.code}), no need to refresh token")
            return null
        }

        Log.w(TAG, "Received 401 Unauthorized - token expired, refreshing...")

        // KROK 2: Získaj aktuálneho používateľa a refresh token
        val user = PreferenceData.getInstance().getUser(context)
        if (user == null) {
            Log.e(TAG, "No user found in SharedPreferences - cannot refresh token")
            PreferenceData.getInstance().clearData(context)
            return null
        }

        Log.d(TAG, "Attempting to refresh token for user: ${user.uid}")

        // KROK 3: Obnov token synchronne (blocking call)
        // Poznámka: Authenticator musí byť synchronný, preto používame blocking verziu
        try {
            val tokenResponse = ApiService.create(context).refreshTokenBlocking(
                userId = user.uid,  // x-user header
                refreshInfo = RefreshTokenRequest(user.refresh)
            ).execute()

            Log.d(TAG, "RefreshToken response code: ${tokenResponse.code()}")

            // KROK 4: Ak je refresh úspešný, ulož nové tokeny
            if (tokenResponse.isSuccessful) {
                tokenResponse.body()?.let { newToken ->
                    Log.d(TAG, "Token refreshed successfully")

                    // Vytvor nový User objekt s novými tokenmi
                    val updatedUser = User(
                        user.name,
                        user.email,
                        user.uid,
                        newToken.access,
                        newToken.refresh,
                        user.photo
                    )

                    // Ulož nové tokeny do SharedPreferences
                    PreferenceData.getInstance().putUser(context, updatedUser)

                    // KROK 5: Vráť nový Request s novým tokenom
                    // OkHttp automaticky retry pôvodnú požiadavku s týmto requestom
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer ${updatedUser.access}")
                        .build()
                }
            } else {
                Log.e(TAG, "Token refresh failed: ${tokenResponse.code()}")
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Exception during token refresh: ${ex.message}", ex)
        }

        // KROK 6: Ak refresh zlyhal, vymaž dáta a odhlás používateľa
        Log.e(TAG, "Token refresh failed - clearing user data and logging out")
        PreferenceData.getInstance().clearData(context)
        return null
    }
}

