# 🔐 OAuth 2.0 Autorizácia - Kompletný Návod

## ✅ Čo sme implementovali:

### **1. Nové API Endpointy**
- ✅ `getUser()` - získanie údajov o používateľovi (vyžaduje Bearer token)
- ✅ `refreshToken()` - obnovenie access tokenu pomocou refresh tokenu

### **2. Data Classes**
- ✅ `RefreshTokenRequest` - request body pre refresh
- ✅ `RefreshTokenResponse` - odpoveď s novými tokenmi
- ✅ `User` - rozšírený o `photo` field

### **3. Automatické obnovenie tokenu**
- ✅ `apiGetUser()` - automaticky obnoví token pri 401 chybe
- ✅ `updateTokens()` - helper metóda na aktualizáciu tokenov

---

## 🔄 OAuth 2.0 Flow v našej aplikácii:

```
┌─────────────────────────────────────────────────────────────┐
│ 1. REGISTRÁCIA / PRIHLÁSENIE                                │
├─────────────────────────────────────────────────────────────┤
│ User → Registrácia                                          │
│   ↓                                                         │
│ API vráti: { uid, access_token, refresh_token }            │
│   ↓                                                         │
│ Uložíme do SharedPreferences                                │
└─────────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. API POŽIADAVKA S TOKENOM                                │
├─────────────────────────────────────────────────────────────┤
│ apiGetUser(uid, my_uid, accessToken, refreshToken)         │
│   ↓                                                         │
│ GET /user/get.php                                           │
│ Headers:                                                    │
│   - x-apikey: API_KEY                                       │
│   - Authorization: Bearer access_token  ← OAuth token       │
└─────────────────────────────────────────────────────────────┘
                    ↓
        ┌───────────┴───────────┐
        │                       │
    ✅ 200 OK              ❌ 401 Unauthorized
        │                       │
        │                       ↓
        │           ┌───────────────────────────────┐
        │           │ TOKEN EXPIROVAL               │
        │           │ Automatické obnovenie         │
        │           └───────────────────────────────┘
        │                       │
        │                       ↓
        │           POST /user/refresh.php
        │           Headers:
        │             - x-apikey: API_KEY
        │             - x-user: my_uid
        │           Body: { refresh: refresh_token }
        │                       │
        │                       ↓
        │           ┌───────────┴───────────┐
        │           │                       │
        │       ✅ 200 OK              ❌ Error
        │           │                       │
        │           ↓                       ↓
        │   Nové tokeny:            Vráť chybu
        │   { access, refresh }     "Please login"
        │           │
        │           ↓
        │   GET /user/get.php
        │   Authorization: Bearer NEW_access_token
        │           │
        │           ↓
        │       ✅ 200 OK
        │           │
        └───────────┴───────────┐
                              ↓
                    Vráť User s novými tokenmi
```

---

## 📋 **Kľúčové koncepty:**

### **1. Bearer Token**

**Čo to je:**
- Access token sa posiela v `Authorization` hlavičke
- Formát: `Authorization: Bearer <access_token>`
- Server overí token a poskytne prístup k zdrojom

**Príklad:**
```kotlin
val headers = mapOf(
    "x-apikey" to API_KEY,
    "Authorization" to "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
)
```

---

### **2. Token Expiration (Expirovanie tokenu)**

**Prečo:**
- Bezpečnosť - ak sa token ukradne, má obmedzenú životnosť
- Access tokeny majú krátku životnosť (napr. 1 hodina)
- Refresh tokeny majú dlhšiu životnosť (napr. 30 dní)

**Čo sa deje:**
```
Access token expiruje → API vráti 401
→ Automaticky použijeme refresh token
→ Získame nový access token
→ Pokračujeme v požiadavke
```

---

### **3. Automatické obnovenie tokenu**

**Výhody:**
- ✅ Používateľ nemusí znova zadávať heslo
- ✅ Seamless UX (bez prerušenia)
- ✅ Bezpečné (refresh token má dlhšiu životnosť)

**Implementácia:**
```kotlin
if (response.code() == 401) {
    // Token expiroval
    val refreshResponse = service.refreshToken(...)
    if (refreshResponse.isSuccessful) {
        // Použij nový token
        val newToken = refreshResponse.body()?.access
        // Skús znova požiadavku
    }
}
```

---

## 🎯 **Použitie v aplikácii:**

### **Príklad: Načítanie profilu používateľa**

```kotlin
// 1. Načítaj uloženého používateľa
val currentUser = PreferenceData.getInstance().getUser(context)
if (currentUser == null) {
    // Používateľ nie je prihlásený
    return
}

// 2. Získaj údaje o používateľovi z API
viewModelScope.launch {
    val (error, user) = repository.apiGetUser(
        uid = "user123",  // UID používateľa ktorého chceme získať
        my_uid = currentUser.uid,  // Náš UID (pre refresh token)
        accessToken = currentUser.access,
        refreshToken = currentUser.refresh
    )
    
    if (user != null) {
        // Ak sa token obnovil, aktualizuj SharedPreferences
        if (user.access != currentUser.access) {
            PreferenceData.getInstance().updateTokens(
                context,
                user.access,
                user.refresh
            )
        }
        
        // Použij údaje o používateľovi
        Log.d("Profile", "User: ${user.name}, Photo: ${user.photo}")
    } else {
        // Chyba
        Log.e("Profile", "Error: $error")
    }
}
```

---

## 🎓 **V React.js terminológii:**

### **OAuth 2.0 Flow**

**Kotlin:**
```kotlin
// 1. Požiadavka s Bearer tokenom
val response = service.getUser(
    mapOf("Authorization" to "Bearer $accessToken"),
    uid
)

// 2. Ak 401, obnov token
if (response.code() == 401) {
    val refreshResponse = service.refreshToken(...)
    val newToken = refreshResponse.body()?.access
    // Skús znova
}
```

**React:**
```javascript
// 1. Požiadavka s Bearer tokenom
const response = await fetch('/api/user/get.php', {
    headers: {
        'Authorization': `Bearer ${accessToken}`,
        'x-apikey': API_KEY
    }
});

// 2. Ak 401, obnov token
if (response.status === 401) {
    const refreshResponse = await fetch('/api/user/refresh.php', {
        method: 'POST',
        headers: {
            'x-apikey': API_KEY,
            'x-user': myUid
        },
        body: JSON.stringify({ refresh: refreshToken })
    });
    
    const { access, refresh } = await refreshResponse.json();
    
    // Ulož nové tokeny
    localStorage.setItem('accessToken', access);
    localStorage.setItem('refreshToken', refresh);
    
    // Skús znova požiadavku
    const retryResponse = await fetch('/api/user/get.php', {
        headers: {
            'Authorization': `Bearer ${access}`,
            'x-apikey': API_KEY
        }
    });
}
```

---

### **Axios Interceptor (Automatické obnovenie)**

**React s Axios:**
```javascript
import axios from 'axios';

// Request interceptor - pridá Bearer token
axios.interceptors.request.use(config => {
    const token = localStorage.getItem('accessToken');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// Response interceptor - automatické obnovenie tokenu
axios.interceptors.response.use(
    response => response,
    async error => {
        if (error.response?.status === 401) {
            // Token expiroval - obnov ho
            const refreshToken = localStorage.getItem('refreshToken');
            const refreshResponse = await axios.post('/api/user/refresh.php', {
                refresh: refreshToken
            });
            
            const { access, refresh } = refreshResponse.data;
            localStorage.setItem('accessToken', access);
            localStorage.setItem('refreshToken', refresh);
            
            // Skús znova pôvodnú požiadavku
            error.config.headers.Authorization = `Bearer ${access}`;
            return axios.request(error.config);
        }
        return Promise.reject(error);
    }
);
```

**Kotlin ekvivalent (OkHttp Interceptor):**
```kotlin
val authInterceptor = Interceptor { chain ->
    val request = chain.request()
    val token = PreferenceData.getInstance().getUser(context)?.access
    
    val newRequest = request.newBuilder()
        .header("Authorization", "Bearer $token")
        .build()
    
    val response = chain.proceed(newRequest)
    
    // Ak 401, obnov token
    if (response.code == 401) {
        // Refresh token logic...
    }
    
    response
}
```

---

## 🔐 **Bezpečnostné poznámky:**

### **1. Token Storage**

**✅ DOBRÉ:**
- SharedPreferences (pre jednoduché aplikácie)
- EncryptedSharedPreferences (pre citlivé dáta)
- KeyStore (pre production aplikácie)

**❌ ZLÉ:**
- Hardcoded v kóde
- Plain text súbory
- Logcat výpisy

---

### **2. Token Lifecycle**

```
Access Token:  Krátka životnosť (1-24 hodín)
    ↓
Expiruje → 401 Unauthorized
    ↓
Refresh Token: Dlhá životnosť (7-30 dní)
    ↓
Získa nový Access Token
    ↓
Pokračuje v požiadavke
```

---

### **3. Refresh Token Rotation**

**Best Practice:**
- Po každom refresh, ulož nový refresh token
- Starý refresh token by mal byť invalidovaný
- Zabráni reuse útokom

**V našom kóde:**
```kotlin
// Po refresh, ulož nové tokeny
PreferenceData.getInstance().updateTokens(
    context,
    newToken.access,
    newToken.refresh  // Nový refresh token
)
```

---

## 📊 **Porovnanie s inými autentifikačnými metódami:**

| Metóda | Bezpečnosť | UX | Komplexnosť |
|--------|-----------|-----|-------------|
| **OAuth 2.0** | ✅✅✅ Vysoká | ✅✅✅ Vynikajúca | ⚠️ Stredná |
| **Session Cookies** | ✅✅ Stredná | ✅✅✅ Vynikajúca | ✅ Nízka |
| **API Keys** | ✅ Nízka | ✅✅ Dobrá | ✅ Nízka |
| **Basic Auth** | ❌ Veľmi nízka | ✅✅ Dobrá | ✅ Nízka |

---

## ✅ **Výhody OAuth 2.0:**

1. ✅ **Bezpečnosť** - Tokeny expirujú, refresh tokeny sa rotujú
2. ✅ **UX** - Automatické obnovenie bez prerušenia
3. ✅ **Štandard** - Široko používaný v priemysle
4. ✅ **Flexibilita** - Rôzne grant types (authorization code, refresh token, atď.)

---

## ❌ **Nevýhody OAuth 2.0:**

1. ❌ **Komplexnosť** - Viac kódu na implementáciu
2. ❌ **Token management** - Musíš sledovať expiráciu
3. ❌ **Error handling** - Musíš riešiť 401 chyby

---

## 🎯 **Zhrnutie:**

**OAuth 2.0 = Standardizovaná autentifikácia**

- **Bearer Token** = Token v Authorization hlavičke
- **Access Token** = Krátkodobý token pre API prístup
- **Refresh Token** = Dlhodobý token pre obnovenie access tokenu
- **Automatické obnovenie** = Seamless UX bez prerušenia

**V našej aplikácii:**
- ✅ Po registrácii získame access + refresh tokeny
- ✅ Tokeny sa ukladajú do SharedPreferences
- ✅ Pri API požiadavkách sa automaticky obnoví token ak expiroval
- ✅ Nové tokeny sa automaticky uložia

---

## 🚀 **Príklad použitia:**

```kotlin
// ViewModel
fun loadUserProfile(uid: String) {
    viewModelScope.launch {
        val currentUser = PreferenceData.getInstance().getUser(context)
            ?: return@launch
        
        val (error, user) = repository.apiGetUser(
            uid = uid,
            my_uid = currentUser.uid,
            accessToken = currentUser.access,
            refreshToken = currentUser.refresh
        )
        
        if (user != null) {
            // Aktualizuj tokeny ak sa zmenili
            if (user.access != currentUser.access) {
                PreferenceData.getInstance().updateTokens(
                    context,
                    user.access,
                    user.refresh
                )
            }
            
            _userProfile.value = user
        } else {
            _error.value = error
        }
    }
}
```

---

**Máš teraz plne funkčnú OAuth 2.0 autentifikáciu s automatickým obnovením tokenu!** 🎉

