# 📱 SharedPreferences - Kompletný Návod

## ✅ Čo sme implementovali:

### **1. User.kt - JSON Serializácia**
- ✅ `toJson()` - konvertuje User → JSON string
- ✅ `fromJson()` - konvertuje JSON string → User

### **2. PreferenceData.kt - Singleton Manager**
- ✅ `putUser()` - uloží používateľa
- ✅ `getUser()` - načíta používateľa
- ✅ `clearData()` - vymaže všetky dáta

### **3. SignupFragment.kt - Integrácia**
- ✅ Po úspešnej registrácii sa používateľ uloží do SharedPreferences

---

## 🎯 Ako používať SharedPreferences:

### **Uloženie používateľa:**
```kotlin
val user = User("Peter", "peter@example.com", "uid123", "access", "refresh")
PreferenceData.getInstance().putUser(context, user)
```

### **Načítanie používateľa:**
```kotlin
val user = PreferenceData.getInstance().getUser(context)
if (user != null) {
    // Používateľ je prihlásený
    Log.d("App", "Logged in user: ${user.name}")
} else {
    // Používateľ nie je prihlásený
    Log.d("App", "No user logged in")
}
```

### **Odhlásenie (vymazanie dát):**
```kotlin
PreferenceData.getInstance().clearData(context)
// alebo
PreferenceData.getInstance().putUser(context, null)
```

---

## 📍 Kde sa dáta ukladajú:

**Cesta na zariadení:**
```
/data/data/com.example.mv11/shared_prefs/eu.mcomputing.mobv.zadanie.xml
```

**Obsah súboru:**
```xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="userKey">{"name":"Peter","email":"peter@example.com","uid":"uid123","access":"access","refresh":"refresh"}</string>
</map>
```

---

## 🔄 Príklad: Načítanie používateľa pri štarte aplikácie

```kotlin
// MainActivity.kt alebo Application class
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val user = PreferenceData.getInstance().getUser(this)
    if (user != null) {
        // Používateľ je prihlásený - presmeruj na hlavnú obrazovku
        Log.d("MainActivity", "User logged in: ${user.name}")
        // Navigácia na FeedFragment
    } else {
        // Používateľ nie je prihlásený - zobraz login screen
        Log.d("MainActivity", "No user - show login")
        // Navigácia na IntroFragment
    }
}
```

---

## 🎓 V React.js terminológii:

### **SharedPreferences = localStorage**

**Kotlin:**
```kotlin
// Uloženie
PreferenceData.getInstance().putUser(context, user)

// Načítanie
val user = PreferenceData.getInstance().getUser(context)
```

**React:**
```javascript
// Uloženie
localStorage.setItem('user', JSON.stringify(user));

// Načítanie
const userJson = localStorage.getItem('user');
const user = userJson ? JSON.parse(userJson) : null;
```

### **PreferenceData = Custom Hook alebo Context**

**Kotlin:**
```kotlin
class PreferenceData {
    fun putUser(context: Context?, user: User?) {
        val editor = sharedPref.edit()
        editor.putString(userKey, user.toJson())
        editor.apply()
    }
}
```

**React:**
```javascript
function usePreferences() {
    const setUser = (user) => {
        localStorage.setItem('user', JSON.stringify(user));
    };
    
    const getUser = () => {
        const json = localStorage.getItem('user');
        return json ? JSON.parse(json) : null;
    };
    
    return { setUser, getUser };
}
```

---

## ⚠️ Dôležité poznámky:

### **1. apply() vs commit()**
```kotlin
editor.apply()  // ✅ Asynchrónne (odporúčané)
editor.commit() // ❌ Synchrónne (môže blokovať UI)
```

### **2. Kedy použiť SharedPreferences:**
- ✅ Malé množstvo dát (nastavenia, session)
- ✅ Jednoduché typy (String, Int, Boolean)
- ✅ Rýchle čítanie/zápis

### **3. Kedy NEPOUŽIŤ SharedPreferences:**
- ❌ Veľké množstvo dát (použi Room Database)
- ❌ Komplexné objekty (použi Room alebo JSON súbory)
- ❌ Citlivé dáta (použi EncryptedSharedPreferences)

---

## 🔐 Bezpečnosť:

**Pre citlivé dáta použite EncryptedSharedPreferences:**
```kotlin
// V build.gradle.kts
implementation("androidx.security:security-crypto:1.1.0-alpha06")

// V kóde
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "secret_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

---

## ✅ Výhody SharedPreferences:

1. ✅ Jednoduché použitie
2. ✅ Automatická synchronizácia medzi komponentami
3. ✅ Perzistentné dáta (prežijú reštart aplikácie)
4. ✅ Rýchle čítanie/zápis

---

## ❌ Nevýhody SharedPreferences:

1. ❌ Nie je vhodné pre veľké množstvo dát
2. ❌ Nie je šifrované (pre citlivé dáta)
3. ❌ Synchrónne operácie (môžu blokovať UI)
4. ❌ Iba primitívne typy (String, Int, Boolean, Float, Long)

---

## 🎯 Zhrnutie:

**SharedPreferences = localStorage v Androide**

- Ukladá jednoduché páry kľúč-hodnota
- Vhodné pre nastavenia a session dáta
- Perzistentné (prežijú reštart)
- Jednoduché na použitie

**V našej aplikácii:**
- Uložíme používateľa po registrácii
- Načítame ho pri štarte aplikácie
- Vymažeme pri odhlásení

