# 🗺️ Testovanie databázy a zobrazenia používateľov na mape

## ✅ Čo bolo implementované:

### 1. **Room Database (SQLite)**
- Tabuľka `users` s používateľmi a ich geolokáciou
- DAO pre CRUD operácie
- LocalCache vrstva
- Offline-first architektúra

### 2. **MapFragment s markermi**
- Automatické načítanie používateľov z API
- Uloženie do lokálnej databázy
- Zobrazenie na mape s červenými markermi
- Každý marker má iniciálu mena používateľa

---

## 🧪 Ako otestovať:

### **Krok 1: Clean & Rebuild**
```
Build → Clean Project
Build → Rebuild Project
```

### **Krok 2: Spusti aplikáciu**
```
Run → Run 'app'
```

### **Krok 3: Prejdi na Map fragment**
- Klikni na ikonu mapy v dolnej navigácii
- **Automaticky sa:**
  1. Zavolá API endpoint `/list.php`
  2. Stiahnu sa dáta o používateľoch
  3. Uložia sa do SQLite databázy
  4. Zobrazia sa markery na mape

### **Krok 4: Skontroluj logy**
```
Logcat → Filter: "MapFragment" alebo "DataRepository"
```

**Očakávaný výstup:**
```
DataRepository: Fetching users from API
DataRepository: Response code: 200
DataRepository: Received X users from API
DataRepository: Users saved to database
MapFragment: Zobrazujem X používateľov na mape
MapFragment: Pridávam marker pre: [Meno] na [lat, lon]
```

### **Krok 5: Skontroluj databázu**
```
View → Tool Windows → App Inspection
→ Database Inspector
→ Vyber aplikáciu
→ Rozbali "mv11DB"
→ Klikni na tabuľku "users"
```

**Uvidíš:**
- Všetky stĺpce: uid, name, updated, lat, lon, radius, photo
- Všetky záznamy z API

---

## 🎯 Čo by si mal vidieť na mape:

1. **Červené kruhy** s bielym písmenom (iniciála mena)
2. **Automatické priblíženie** na prvého používateľa
3. **Snackbar správa** "Používatelia načítaní!"

---

## 🔧 Riešenie problémov:

### **Problém: Žiadne markery na mape**

**Riešenie:**
1. Skontroluj internet pripojenie
2. Pozri Logcat pre chyby
3. Skontroluj či API endpoint funguje:
   ```
   https://zadanie.mpage.sk/list.php
   Header: x-apikey: c95332ee022df8c953ce470261efc695ecf3e784
   ```

### **Problém: Tabuľka users neexistuje**

**Riešenie:**
- To je normálne pred prvým spustením!
- Tabuľka sa vytvorí automaticky pri prvom prístupe
- Prejdi na Map fragment a počkaj 2-3 sekundy

### **Problém: "Failed to load users"**

**Riešenie:**
1. Skontroluj API key
2. Skontroluj internet
3. Pozri error v Logcat

---

## 📊 Architektúra toku dát:

```
MapFragment
    ↓ onViewCreated
UserFeedViewModel
    ↓ feed_items (liveData)
    ↓ automaticky zavolá:
DataRepository.apiListGeofence()
    ↓ HTTP GET
API Server (zadanie.mpage.sk/list.php)
    ↓ Response<List<UserResponse>>
    ↓ konverzia na UserEntity
LocalCache.insertUserItems()
    ↓ Room Database
SQLite Database (mv11DB)
    ↓ LiveData observe
    ↓ emitSource
MapFragment.setupUserMarkers()
    ↓ vytvorenie markerov
Mapbox Map (UI)
```

---

## 🎨 Vzhľad markerov:

- **Červený kruh** - farba markera
- **Biele písmeno** - prvé písmeno mena používateľa
- **Veľkosť**: 100x100px

---

## 🚀 Ďalšie vylepšenia:

1. **Refresh button** - manuálna aktualizácia
2. **Click na marker** - zobrazenie detailov používateľa
3. **Zobrazenie radius** - kruh okolo markera
4. **Rôzne farby** - podľa stavu používateľa
5. **Clustrovanie** - pri viacerých markeroch blízko seba

---

## 📱 Offline režim:

**Ak nemáš internet:**
- Aplikácia zobrazí posledné uložené dáta z databázy
- Pri obnovení internetu sa automaticky aktualizuje

**Výhody:**
- ✅ Rýchle načítanie (z lokálnej DB)
- ✅ Funguje offline
- ✅ Automatická synchronizácia

---

## 🎓 V React terminológii:

```javascript
function MapComponent() {
    const { feedItems, loading } = useFeedViewModel();
    
    useEffect(() => {
        // Fetch from API
        fetchUsers().then(users => {
            // Save to IndexedDB
            db.users.bulkPut(users);
        });
    }, []);
    
    useEffect(() => {
        // Display markers when data changes
        feedItems.forEach(user => {
            addMarker(user.lat, user.lon, user.name);
        });
    }, [feedItems]);
}
```

---

## ✅ Checklist:

- [ ] Aplikácia sa spustila bez chýb
- [ ] Map fragment sa načítal
- [ ] V Logcat vidím "Fetching users from API"
- [ ] V Logcat vidím "Users saved to database"
- [ ] Na mape vidím markery
- [ ] V Database Inspector vidím tabuľku "users"
- [ ] Tabuľka obsahuje dáta

---

**Ak všetko funguje, gratulujem! Máš funkčnú offline-first aplikáciu s geolokáciou! 🎉**

