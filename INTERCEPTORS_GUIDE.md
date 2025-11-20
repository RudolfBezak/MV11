# 🔐 Pokročilá Autorizácia s Interceptormi - Kompletný Návod

## ✅ Čo sme implementovali:

### **1. AuthInterceptor**
- ✅ Automaticky pridáva `Authorization: Bearer <token>` hlavičku
- ✅ Automaticky pridáva `x-apikey` hlavičku
- ✅ Pridáva `Accept` a `Content-Type` hlavičky

### **2. TokenAuthenticator**
- ✅ Automaticky detekuje 401 chyby
- ✅ Automaticky obnovuje access token
- ✅ Retry pôvodnú požiadavku s novým tokenom
- ✅ Logout ak refresh zlyhá

### **3. Zjednodušené API volania**
- ✅ Už nemusíš manuálne pridávať tokeny
- ✅ Už nemusíš riešiť 401 chyby
- ✅ Všetko sa deje automaticky!

---

## 🔄 Ako to funguje - Request Flow:

```
┌─────────────────────────────────────────────────────────────┐
│ 1. API VOLANIE (v kóde)                                      │
├─────────────────────────────────────────────────────────────┤
│ service.getUser(uid)  ← Jednoduché volanie                  │
└─────────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. AUTH INTERCEPTOR (pridá hlavičky)                        │
├─────────────────────────────────────────────────────────────┤
│ AuthInterceptor.intercept()                                  │
│   ├─ Pridá "Accept: application/json"                       │
│   ├─ Pridá "Content-Type: application/json"                │
│   ├─ Pridá "Authorization: Bearer <token>"  ← Z SharedPrefs │
│   └─ Pridá "x-apikey: <API_KEY>"                           │
└─────────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. HTTP REQUEST (na server)                                  │
├─────────────────────────────────────────────────────────────┤
│ GET /user/get.php?id=user123                                 │
│ Headers:                                                     │
│   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9│
│   x-apikey: c95332ee022df8c953ce470261efc695ecf3e784        │
└─────────────────────────────────────────────────────────────┘
                    ↓
        ┌───────────┴───────────┐
        │                       │
    ✅ 200 OK              ❌ 401 Unauthorized
        │                       │
        │                       ↓
        │           ┌───────────────────────────────┐
        │           │ TOKEN AUTHENTICATOR           │
        │           │ Automaticky sa zavolá         │
        │           └───────────────────────────────┘
        │                       │
        │                       ↓
        │           POST /user/refresh.php
        │           Body: { refresh: refresh_token }
        │                       │
        │                       ↓
        │           ┌───────────┴───────────┐
        │           │                       │
        │       ✅ 200 OK              ❌ Error
        │           │                       │
        │           ↓                       ↓
        │   Uloží nové tokeny        Vymaže dáta
        │   do SharedPreferences     (logout)
        │           │                       │
        │           ↓                       ↓
        │   Retry pôvodnú           Vráť null
        │   požiadavku s novým      (chyba)
        │   tokenom
        │           │
        │           ↓
        │       ✅ 200 OK
        │           │
        └───────────┴───────────┐
                              ↓
                    Vráť Response<UserResponse>
```

---

## 📋 **Porovnanie: PRED vs PO**

### **PRED (Manuálne pridávanie tokenov):**

```kotlin
// ❌ Zložité - musíš manuálne pridávať tokeny
suspend fun apiGetUser(uid: String, accessToken: String): Pair<String, User?> {
    val response = service.getUser(
        mapOf(
            "x-apikey" to API_KEY,
            "Authorization" to "Bearer $accessToken"
        ),
        uid
    )
    
    if (response.code() == 401) {
        // Manuálne obnovenie tokenu...
        val refreshResponse = service.refreshToken(...)
        // Retry požiadavku...
    }
}
```

### **PO (Automatické interceptory):**

```kotlin
// ✅ Jednoduché - tokeny sa pridajú automaticky!
suspend fun apiGetUser(uid: String): Pair<String, User?> {
    val response = service.getUser(uid)  // ← To je všetko!
    
    if (response.isSuccessful) {
        // Token sa automaticky obnovil ak expiroval!
        return Pair("", User(...))
    }
}
```

---

## 🎯 **Kľúčové Komponenty:**

### **1. AuthInterceptor - Pridáva hlavičky**

```kotlin
class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
        
        // Získaj token z SharedPreferences
        val token = PreferenceData.getInstance().getUser(context)?.access
        
        // Pridaj Authorization header ak token existuje
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        
        // Pridaj API key
        requestBuilder.addHeader("x-apikey", API_KEY)
        
        return chain.proceed(requestBuilder.build())
    }
}
```

**Čo robí:**
- ✅ Pridá JSON hlavičky
- ✅ Pridá Authorization header (ak token existuje)
- ✅ Pridá API key

---

### **2. TokenAuthenticator - Obnovuje tokeny**

```kotlin
class TokenAuthenticator(private val context: Context) : Authenticator {
    override fun authenticate(route: Route?, response: okhttp3.Response): Request? {
        if (response.code != 401) return null
        
        // Získaj refresh token
        val user = PreferenceData.getInstance().getUser(context) ?: return null
        
        // Obnov token synchronne
        val refreshResponse = ApiService.create(context)
            .refreshTokenBlocking(RefreshTokenRequest(user.refresh))
            .execute()
        
        if (refreshResponse.isSuccessful) {
            val newToken = refreshResponse.body()!!
            
            // Ulož nové tokeny
            PreferenceData.getInstance().putUser(context, 
                user.copy(access = newToken.access, refresh = newToken.refresh))
            
            // Vráť nový request s novým tokenom
            return response.request.newBuilder()
                .header("Authorization", "Bearer ${newToken.access}")
                .build()
        }
        
        // Ak refresh zlyhal, logout
        PreferenceData.getInstance().clearData(context)
        return null
    }
}
```

**Čo robí:**
- ✅ Detekuje 401 chyby
- ✅ Automaticky obnoví token
- ✅ Retry pôvodnú požiadavku
- ✅ Logout ak refresh zlyhá

---

## 🎓 **V React.js terminológii:**

### **AuthInterceptor = Axios Request Interceptor**

**Kotlin:**
```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(AuthInterceptor(context))
    .build()
```

**React (Axios):**
```javascript
axios.interceptors.request.use(config => {
    const token = localStorage.getItem('accessToken');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    config.headers['x-apikey'] = API_KEY;
    return config;
});
```

---

### **TokenAuthenticator = Axios Response Interceptor**

**Kotlin:**
```kotlin
val client = OkHttpClient.Builder()
    .authenticator(TokenAuthenticator(context))
    .build()
```

**React (Axios):**
```javascript
axios.interceptors.response.use(
    response => response,
    async error => {
        if (error.response?.status === 401) {
            const refreshToken = localStorage.getItem('refreshToken');
            const { data } = await axios.post('/api/refresh', { refresh: refreshToken });
            
            localStorage.setItem('accessToken', data.access);
            localStorage.setItem('refreshToken', data.refresh);
            
            // Retry pôvodnú požiadavku
            error.config.headers.Authorization = `Bearer ${data.access}`;
            return axios.request(error.config);
        }
        return Promise.reject(error);
    }
);
```

---

## 📊 **Porovnanie Interceptor vs Manuálne:**

| Vlastnosť | Manuálne | Interceptory |
|-----------|----------|--------------|
| **Kód v každej metóde** | ❌ Áno | ✅ Nie |
| **Automatické obnovenie** | ❌ Nie | ✅ Áno |
| **Centralizovaná logika** | ❌ Nie | ✅ Áno |
| **Zložitosť** | ❌ Vysoká | ✅ Nízka |
| **Chyby** | ❌ Časté | ✅ Zriedkavé |

---

## 🔍 **Detailný Flow:**

### **Príklad: Volanie `getUser()`**

```kotlin
// 1. V kóde (jednoduché)
val response = service.getUser("user123")

// 2. AuthInterceptor sa automaticky zavolá
//    → Pridá Authorization: Bearer <token>
//    → Pridá x-apikey: <key>

// 3. Request ide na server
GET /user/get.php?id=user123
Headers:
  Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
  x-apikey: c95332ee022df8c953ce470261efc695ecf3e784

// 4. Server odpovie
//    a) 200 OK → Vráť User
//    b) 401 Unauthorized → TokenAuthenticator sa zavolá

// 5. Ak 401:
//    → TokenAuthenticator.refreshTokenBlocking()
//    → Uloží nové tokeny
//    → Retry pôvodnú požiadavku
//    → Vráť User s novými tokenmi
```

---

## ✅ **Výhody Interceptorov:**

1. ✅ **DRY Principle** - Don't Repeat Yourself
   - Tokeny sa pridávajú na jednom mieste
   - Nie v každej metóde

2. ✅ **Automatické obnovenie**
   - Nemusíš riešiť 401 chyby
   - Všetko sa deje automaticky

3. ✅ **Čistejší kód**
   - API volania sú jednoduchšie
   - Menej boilerplate kódu

4. ✅ **Centralizovaná logika**
   - Zmena autentifikácie na jednom mieste
   - Ľahšie testovanie

---

## ⚠️ **Dôležité poznámky:**

### **1. Poradie Interceptorov:**

```kotlin
OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)  // 1. Prvý - vidí request pred zmenou
    .addInterceptor(authInterceptor)      // 2. Druhý - pridá tokeny
    .authenticator(tokenAuthenticator)    // 3. Tretí - obnoví token pri 401
    .build()
```

**Poradie je dôležité!**

---

### **2. Blocking vs Suspend:**

**refreshTokenBlocking()** - musí byť synchronný (Call)
- Používa sa v TokenAuthenticator
- Authenticator musí vrátiť Request synchronne

**refreshToken()** - môže byť asynchrónny (suspend)
- Používa sa v normálnych coroutine volaniach

---

### **3. Context v ApiService:**

```kotlin
fun create(context: Context): ApiService {
    // Context je potrebný pre:
    // - AuthInterceptor (SharedPreferences)
    // - TokenAuthenticator (SharedPreferences)
}
```

---

## 🎯 **Príklady použitia:**

### **Pred (komplikované):**
```kotlin
suspend fun apiGetUser(uid: String, accessToken: String): Pair<String, User?> {
    val response = service.getUser(
        mapOf(
            "x-apikey" to API_KEY,
            "Authorization" to "Bearer $accessToken"
        ),
        uid
    )
    
    if (response.code() == 401) {
        // Manuálne refresh...
    }
}
```

### **Po (jednoduché):**
```kotlin
suspend fun apiGetUser(uid: String): Pair<String, User?> {
    val response = service.getUser(uid)  // ← To je všetko!
    
    if (response.isSuccessful) {
        return Pair("", User(...))
    }
}
```

---

## 🚀 **Zhrnutie:**

**Interceptory = Middleware v HTTP requestoch**

- **AuthInterceptor** = Automaticky pridáva tokeny
- **TokenAuthenticator** = Automaticky obnovuje tokeny
- **Výsledok** = Jednoduchší a čistejší kód

**V našej aplikácii:**
- ✅ Každá požiadavka má automaticky token
- ✅ Token sa automaticky obnoví ak expiroval
- ✅ Kód je jednoduchší a čistejší
- ✅ Menej chýb a lepšia UX

---

**Máš teraz pokročilú autorizáciu s automatickým obnovením tokenov!** 🎉

