package com.example.mv11

import android.content.Context
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

/**
 * AuthInterceptor - automaticky pridáva autentifikačné hlavičky do každého HTTP requestu.
 * 
 * Účel:
 * - Automaticky pridá "Authorization: Bearer <token>" hlavičku
 * - Automaticky pridá "x-apikey" hlavičku
 * - Pridá "Accept" a "Content-Type" hlavičky pre JSON
 * 
 * Výhody:
 * - Nemusíš manuálne pridávať tokeny do každej požiadavky
 * - Centralizovaná logika autentifikácie
 * - Konzistentné hlavičky vo všetkých requestoch
 * 
 * @param context - kontext aplikácie pre prístup k SharedPreferences
 */
class AuthInterceptor(private val context: Context) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
    }

    /**
     * Interceptuje HTTP požiadavku pred odoslaním na server.
     * 
     * @param chain - Interceptor.Chain obsahujúci request a response
     * @return Response - odpoveď zo servera
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        // KROK 1: Vytvor nový request builder na základe pôvodného requestu
        val requestBuilder = chain.request().newBuilder()
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")

        // KROK 2: Získaj access token z SharedPreferences
        val token = PreferenceData.getInstance().getUser(context)?.access

        // KROK 3: Pridaj Authorization hlavičku ak token existuje
        if (token != null && token.isNotEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
            Log.d(TAG, "Added Authorization header with token")
        } else {
            Log.w(TAG, "No access token found - request will be unauthenticated")
        }

        // KROK 4: Pridaj API kľúč do každej požiadavky
        requestBuilder.addHeader("x-apikey", DataRepository.API_KEY)

        // KROK 5: Vykonaj požiadavku s novými hlavičkami
        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}

