# 📱 Sprievodca Android Navigáciou s Fragmentami

## 🎯 Čo je to Fragment?

**Fragment** = menšia časť obrazovky (ako puzzle kúsok), ktorá sa zobrazuje v `Activity`. 
- Môžete mať veľa fragmentov, ale len jednu hlavnú `Activity`
- Fragmenty sa dajú ľahko vymieňať bez vytvárania nových aktivít

---

## 🗺️ Ako funguje navigácia?

### 1. **MainActivity** (hlavná aktivita)
```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
```
- Toto je váš hlavný "kontajner"
- Spustí sa pri štarte aplikácie
- Obsahuje `NavHostFragment` (pozri nižšie)

---

### 2. **activity_main.xml** (layout hlavnej aktivity)
```xml
<fragment
    android:id="@+id/nav_host_fragment_activity_main"
    android:name="androidx.navigation.fragment.NavHostFragment"
    app:navGraph="@navigation/nav_graph" />
```
**Čo to robí:**
- `NavHostFragment` = priestor, kde sa zobrazujú jednotlivé fragmenty
- `navGraph` = odkaz na navigačný graf, ktorý definuje všetky fragmenty

---

### 3. **nav_graph.xml** (navigačný graf)
```xml
<navigation 
    app:startDestination="@+id/introFragment">  <!-- tu sa app spustí -->
    
    <fragment
        android:id="@+id/introFragment"
        android:name="com.example.mv11.IntroFragment">  <!-- MUSÍ BYŤ plný názov triedy -->
        
        <action
            android:id="@+id/action_intro_to_prihlasenie"
            app:destination="@id/prihlasenieFragment" />
    </fragment>
    
    <fragment
        android:id="@+id/prihlasenieFragment"
        android:name="com.example.mv11.PrihlasenieFragment" />
</navigation>
```

**Dôležité časti:**
- `startDestination` = prvý fragment, ktorý sa zobrazí
- `android:name` = plný názov triedy fragmentu (package + názov)
- `<action>` = definuje prechod z jedného fragmentu do druhého

---

### 4. **Fragment triedy** (Kotlin kód)

#### Štruktúra fragmentu:
```kotlin
class IntroFragment : Fragment() {
    
    // Táto metóda vytvorí UI fragmentu
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_intro, container, false)
    }
    
    // Táto metóda sa volá keď je UI vytvorené
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Tu môžete pridať listenery na tlačidlá
        view.findViewById<Button>(R.id.button1).setOnClickListener {
            findNavController().navigate(R.id.action_intro_to_prihlasenie)
        }
    }
}
```

**Metódy:**
- `onCreateView()` = vytvorí UI z XML súboru
- `onViewCreated()` = volá sa keď je UI hotové, tu pridávate onClick listenery

---

## 🚀 Ako navigovať medzi fragmentami?

### V kóde fragmentu:
```kotlin
findNavController().navigate(R.id.action_intro_to_prihlasenie)
```

**Čo to robí:**
1. `findNavController()` = nájde navigačný kontrolér
2. `navigate()` = prejde na fragment definovaný v `nav_graph.xml`
3. `R.id.action_intro_to_prihlasenie` = ID akcie z `nav_graph.xml`

---

## 📂 Štruktúra vašej aplikácie

```
app/
├── MainActivity.kt                      ← hlavná aktivita (spúšťa sa pri štarte)
│
├── res/
│   ├── layout/
│   │   ├── activity_main.xml           ← obsahuje NavHostFragment
│   │   ├── fragment_intro.xml          ← UI pre IntroFragment
│   │   ├── fragment_prihlasenie.xml    ← UI pre PrihlasenieFragment
│   │   └── fragment_signup.xml         ← UI pre SignupFragment
│   │
│   └── navigation/
│       └── nav_graph.xml               ← definuje všetky fragmenty a prechody
│
└── java/.../mv11/
    ├── IntroFragment.kt                ← fragment úvodnej obrazovky
    ├── PrihlasenieFragment.kt          ← fragment prihlásenia
    └── SignupFragment.kt               ← fragment registrácie
```

---

## 🔄 Tok navigácie vo vašej aplikácii

```
MainActivity spustí → nav_graph.xml → IntroFragment (úvodná obrazovka)
                                           |
                                           |-- tlačidlo "Prihlásenie" → PrihlasenieFragment
                                           |
                                           |-- tlačidlo "Registrácia" → SignupFragment
                                                                            |
                                                                            |-- tlačidlo "Odoslať" → späť na IntroFragment
```

---

## ✅ Opravené chyby vo vašom kóde:

### ❌ **Pred opravou:**
1. `nav_graph.xml` odkazoval na neexistujúce fragmenty (`com.example.FirstFragment`)
2. Fragment `prihlasenie` nemal `onCreateView()` metódu
3. Používali ste `InputActivity` namiesto fragmentov
4. Duplicitné akcie v `nav_graph.xml`

### ✅ **Po oprave:**
1. Vytvorené správne fragmenty: `IntroFragment`, `PrihlasenieFragment`, `SignupFragment`
2. Každý fragment má `onCreateView()` a `onViewCreated()`
3. Vymazaná `InputActivity` (nahradená `SignupFragment`)
4. Opravený `nav_graph.xml` so správnymi odkazmi

---

## 🎓 Ako pridať nový fragment?

### Krok 1: Vytvorte layout XML
```xml
<!-- fragment_novy.xml -->
<LinearLayout>
    <TextView android:text="Nový fragment" />
</LinearLayout>
```

### Krok 2: Vytvorte Kotlin triedu
```kotlin
class NovyFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_novy, container, false)
    }
}
```

### Krok 3: Pridajte do nav_graph.xml
```xml
<fragment
    android:id="@+id/novyFragment"
    android:name="com.example.mv11.NovyFragment"
    android:label="Nový" />
```

### Krok 4: Pridajte akciu (prechod)
```xml
<action
    android:id="@+id/action_intro_to_novy"
    app:destination="@id/novyFragment" />
```

### Krok 5: Použite v kóde
```kotlin
findNavController().navigate(R.id.action_intro_to_novy)
```

---

## 📚 Užitočné príkazy

### Získať NavController v fragmente:
```kotlin
findNavController()
```

### Navigovať na fragment:
```kotlin
findNavController().navigate(R.id.akcia)
```

### Vrátiť sa späť:
```kotlin
findNavController().navigateUp()
```

### Odoslať dáta do fragmentu:
```kotlin
val bundle = Bundle()
bundle.putString("meno", "Jano")
findNavController().navigate(R.id.akcia, bundle)
```

### Prijať dáta vo fragmente:
```kotlin
val meno = arguments?.getString("meno")
```

---

## 🐛 Časté chyby

### 1. "Navigation destination is unknown"
❌ **Problém:** ID akcie alebo fragmentu neexistuje v `nav_graph.xml`
✅ **Riešenie:** Skontrolujte, že ID v kóde sedí s ID v `nav_graph.xml`

### 2. "Fragment not found"
❌ **Problém:** Zlý názov triedy v `android:name`
✅ **Riešenie:** Použite plný názov: `com.example.mv11.IntroFragment`

### 3. "InflateException"
❌ **Problém:** Chyba v XML layoute
✅ **Riešenie:** Skontrolujte syntax XML súborov

---

## 🎉 Teraz by všetko malo fungovať!

Spustite aplikáciu a mali by ste vidieť:
1. **IntroFragment** - úvodná obrazovka s dvoma tlačidlami
2. Kliknutím na "Prihlásenie" → **PrihlasenieFragment**
3. Kliknutím na "Registrácia" → **SignupFragment**
4. Po vyplnení formulára v SignupFragment → späť na **IntroFragment**

