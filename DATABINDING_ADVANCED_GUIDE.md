# 📱 Jednosmerný a Obojsmerný DataBinding - Kompletný Návod

## ✅ Čo sme implementovali:

### **1. Jednosmerný DataBinding (One-way)**
- ✅ `@{viewModel.displayText}` - dáta tečú z ViewModelu do UI
- ✅ Automatická aktualizácia TextView pri zmene dát

### **2. Obojsmerný DataBinding (Two-way)**
- ✅ `@={viewModel.userInput}` - dáta tečú v oboch smeroch
- ✅ Synchronizácia medzi EditText a ViewModel

### **3. Click Eventy v XML**
- ✅ `android:onClick="@{() -> viewModel.method()}"`
- ✅ Metódy sa volajú priamo z XML bez potreby listenerov v kóde

---

## 🔄 Jednosmerný vs Obojsmerný Binding:

### **Jednosmerný Binding (`@{ }`):**

```
ViewModel → UI
```

**Syntax:**
```xml
<TextView android:text="@{viewModel.displayText}" />
```

**Ako to funguje:**
1. ViewModel má `displayText` LiveData
2. Zmena `displayText.value = "Nový text"`
3. TextView sa automaticky aktualizuje
4. UI nemôže zmeniť ViewModel (jednosmerný tok)

**Príklad:**
```kotlin
// ViewModel
val displayText = MutableLiveData<String>()

// XML
<TextView android:text="@{viewModel.displayText}" />

// Kotlin
viewModel.displayText.value = "Ahoj!"  // TextView sa automaticky aktualizuje
```

---

### **Obojsmerný Binding (`@={ }`):**

```
ViewModel ↔ UI
```

**Syntax:**
```xml
<EditText android:text="@={viewModel.userInput}" />
```

**Ako to funguje:**
1. ViewModel má `userInput` MutableLiveData
2. Používateľ zadá text do EditText
3. `userInput.value` sa automaticky aktualizuje
4. Zmena `userInput.value` sa automaticky zobrazí v EditText
5. Dáta tečú v oboch smeroch (obojsmerný tok)

**Príklad:**
```kotlin
// ViewModel
val userInput = MutableLiveData<String>()

// XML
<EditText android:text="@={viewModel.userInput}" />

// Používateľ zadá "Ahoj" do EditText
// → userInput.value sa automaticky nastaví na "Ahoj"

// Kotlin
viewModel.userInput.value = "Nový text"  // EditText sa automaticky aktualizuje
```

---

## 📋 **Porovnanie:**

| Vlastnosť | Jednosmerný (`@{ }`) | Obojsmerný (`@={ }`) |
|-----------|---------------------|---------------------|
| **Smer toku dát** | ViewModel → UI | ViewModel ↔ UI |
| **Použitie** | TextView, ImageView | EditText, CheckBox, Switch |
| **UI môže zmeniť ViewModel** | ❌ Nie | ✅ Áno |
| **ViewModel môže zmeniť UI** | ✅ Áno | ✅ Áno |
| **Syntax** | `@{ }` | `@={ }` |

---

## 🎯 **Click Eventy v XML:**

### **Syntax:**

```xml
<!-- Bez parametrov -->
<Button android:onClick="@{() -> viewModel.onButtonClick()}" />

<!-- S parametrom (View) -->
<Button android:onClick="@{(view) -> viewModel.onButtonClick(view)}" />

<!-- S parametrom (Item) -->
<Button android:onClick="@{(item) -> viewModel.onItemClick(item)}" />
```

### **Príklad:**

**XML:**
```xml
<Button
    android:id="@+id/btnAdd"
    android:text="Pridať"
    android:onClick="@{() -> viewModel.onAddButtonClick()}" />
```

**ViewModel:**
```kotlin
fun onAddButtonClick() {
    val newItem = MyItem(1, R.drawable.icon, "Nová položka")
    addItem(newItem)
}
```

**Výsledok:**
- ✅ Žiadny onClick listener v Fragmente
- ✅ Metóda sa volá priamo z XML
- ✅ Čistejší kód

---

## 🎓 **V React.js terminológii:**

### **Jednosmerný Binding = Controlled Component (Read-only)**

**Kotlin (DataBinding):**
```xml
<!-- XML -->
<TextView android:text="@{viewModel.displayText}" />
```

**React:**
```jsx
// JSX
function Component() {
    const [displayText, setDisplayText] = useState("Ahoj");
    return <TextView text={displayText} />;
}
```

**Porovnanie:**
- ✅ Dáta tečú z state do UI
- ✅ UI nemôže zmeniť state priamo
- ✅ Jednosmerný tok dát

---

### **Obojsmerný Binding = Controlled Component (Read-write)**

**Kotlin (DataBinding):**
```xml
<!-- XML -->
<EditText android:text="@={viewModel.userInput}" />
```

**React:**
```jsx
// JSX
function Component() {
    const [userInput, setUserInput] = useState("");
    
    return (
        <EditText 
            value={userInput}
            onChange={(e) => setUserInput(e.target.value)} 
        />
    );
}
```

**Porovnanie:**
- ✅ Dáta tečú v oboch smeroch
- ✅ UI môže zmeniť state
- ✅ State môže zmeniť UI
- ✅ Obojsmerný tok dát

---

### **Click Eventy v XML = JSX onClick**

**Kotlin (DataBinding):**
```xml
<!-- XML -->
<Button android:onClick="@{() -> viewModel.onButtonClick()}" />
```

**React:**
```jsx
// JSX
<Button onClick={() => viewModel.onButtonClick()} />
```

**Porovnanie:**
- ✅ Event handler priamo v XML/JSX
- ✅ Volanie metódy bez potreby listenera
- ✅ Čistejší kód

---

## 📊 **Kompletný Príklad:**

### **XML Layout:**

```xml
<layout xmlns:android="http://schemas.android.com/apk/res/android">
    <data>
        <variable name="viewModel" type="com.example.FeedViewModel" />
    </data>
    
    <ConstraintLayout>
        <!-- JEDNOSMERNÝ BINDING -->
        <TextView
            android:text="@{viewModel.displayText}" />
        
        <!-- OBOJSMERNÝ BINDING -->
        <EditText
            android:text="@={viewModel.userInput}" />
        
        <!-- CLICK EVENT V XML -->
        <Button
            android:onClick="@{() -> viewModel.onButtonClick()}" />
    </ConstraintLayout>
</layout>
```

---

### **ViewModel:**

```kotlin
class FeedViewModel : ViewModel() {
    // Jednosmerný binding
    val displayText = MutableLiveData<String>()
    
    // Obojsmerný binding
    val userInput = MutableLiveData<String>()
    
    init {
        displayText.value = "Zadajte text..."
        userInput.value = ""
    }
    
    // Click event metóda
    fun onButtonClick() {
        displayText.value = "Klikli ste na tlačidlo!"
    }
}
```

---

### **Fragment:**

```kotlin
class FeedFragment : Fragment() {
    private var binding: FragmentFeedBinding? = null
    private lateinit var viewModel: FeedViewModel
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentFeedBinding.bind(view).apply {
            lifecycleOwner = viewLifecycleOwner
            this.viewModel = viewModel
        }
        
        // Pozorovanie zmien (voliteľné - UI sa aktualizuje automaticky)
        viewModel.userInput.observe(viewLifecycleOwner) { input ->
            Log.d("Fragment", "User input: $input")
        }
    }
}
```

---

## ✅ **Výhody:**

### **Jednosmerný Binding:**
1. ✅ Jednoduchý a bezpečný
2. ✅ Ideálny pre zobrazenie dát
3. ✅ Menej rizika chýb

### **Obojsmerný Binding:**
1. ✅ Automatická synchronizácia
2. ✅ Ideálny pre formuláre
3. ✅ Menej boilerplate kódu

### **Click Eventy v XML:**
1. ✅ Menej kódu v Fragmente
2. ✅ Čistejší XML
3. ✅ Jednoduchšia údržba

---

## ⚠️ **Dôležité poznámky:**

### **1. Kedy použiť jednosmerný binding:**
- ✅ TextView (zobrazenie textu)
- ✅ ImageView (zobrazenie obrázka)
- ✅ Všetky read-only komponenty

### **2. Kedy použiť obojsmerný binding:**
- ✅ EditText (vstup textu)
- ✅ CheckBox (checkbox stav)
- ✅ Switch (switch stav)
- ✅ Všetky interaktívne komponenty

### **3. Rozdiel v syntax:**
```xml
<!-- Jednosmerný -->
android:text="@{viewModel.text}"

<!-- Obojsmerný -->
android:text="@={viewModel.text}"
```

**Poznámka:** `@={ }` má `=` znak!

---

## 🎯 **Príklady použitia:**

### **1. Formulár s obojsmerným bindingom:**

```xml
<EditText
    android:hint="Meno"
    android:text="@={viewModel.name}" />

<EditText
    android:hint="Email"
    android:text="@={viewModel.email}" />

<Button
    android:text="Odoslať"
    android:onClick="@{() -> viewModel.submitForm()}" />
```

---

### **2. Zobrazenie dát s jednosmerným bindingom:**

```xml
<TextView
    android:text="@{viewModel.userName}" />

<TextView
    android:text="@{viewModel.userEmail}" />

<ImageView
    android:src="@{viewModel.userPhoto}" />
```

---

### **3. Kombinácia jednosmerného a obojsmerného:**

```xml
<!-- Používateľ zadá text -->
<EditText android:text="@={viewModel.userInput}" />

<!-- Text sa zobrazí -->
<TextView android:text="@{viewModel.userInput}" />
```

**Výsledok:**
- Používateľ zadá text do EditText
- TextView sa automaticky aktualizuje
- Všetko bez kódu!

---

## 🚀 **Zhrnutie:**

**Jednosmerný Binding (`@{ }`):**
- ViewModel → UI
- Pre zobrazenie dát
- TextView, ImageView

**Obojsmerný Binding (`@={ }`):**
- ViewModel ↔ UI
- Pre formuláre
- EditText, CheckBox, Switch

**Click Eventy (`android:onClick`):**
- Metódy sa volajú z XML
- Menej kódu v Fragmente
- Čistejší kód

**V našej aplikácii:**
- ✅ TextView s jednosmerným bindingom
- ✅ EditText s obojsmerným bindingom
- ✅ Buttony s click eventmi v XML
- ✅ Automatická synchronizácia dát

---

**Máš teraz plne funkčný jednosmerný a obojsmerný DataBinding!** 🎉

