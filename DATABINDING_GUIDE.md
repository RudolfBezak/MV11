# 📱 DataBinding - Kompletný Návod

## ✅ Čo sme implementovali:

### **1. Pridanie DataBinding do projektu**
- ✅ `dataBinding = true` v `build.gradle.kts`
- ✅ Automatická generácia binding tried

### **2. Upravenie XML layoutu**
- ✅ `<layout>` tag obalí celý layout
- ✅ `<data>` sekcia definuje ViewModel premennú
- ✅ ViewModel dostupný v XML

### **3. Upravenie Fragmentu**
- ✅ Použitie `FragmentFeedBinding` namiesto `findViewById`
- ✅ Nastavenie `lifecycleOwner` a `viewModel`
- ✅ Čistejší a jednoduchší kód

---

## 🔄 Ako DataBinding funguje:

### **1. Kompilácia:**

```
fragment_feed.xml
    ↓
Android Studio generuje
    ↓
FragmentFeedBinding.kt (automaticky)
    ↓
Obsahuje:
- binding.btnAdd (Button)
- binding.feedRecyclerview (RecyclerView)
- binding.viewModel (FeedViewModel)
- binding.lifecycleOwner (LifecycleOwner)
```

### **2. Runtime:**

```
FragmentFeedBinding.inflate()
    ↓
Vytvorí binding objekt
    ↓
binding.viewModel = viewModel
    ↓
XML má prístup k viewModel
    ↓
Automatická aktualizácia UI pri zmene dát
```

---

## 📋 **Porovnanie: PRED vs PO**

### **PRED (Bez DataBinding):**

```kotlin
// ❌ Veľa boilerplate kódu
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val btnAdd = view.findViewById<Button>(R.id.btnAdd)
    val btnRemove = view.findViewById<Button>(R.id.btnRemove)
    val recyclerView = view.findViewById<RecyclerView>(R.id.feed_recyclerview)
    
    btnAdd.setOnClickListener { ... }
    btnRemove.setOnClickListener { ... }
    
    // Manuálne aktualizácie UI
    viewModel.feedItems.observe(this) { items ->
        adapter.updateItems(items)
        // Musíš manuálne volať adapter.updateItems()
    }
}
```

### **PO (S DataBinding):**

```kotlin
// ✅ Čistejší kód
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    binding = FragmentFeedBinding.bind(view).apply {
        lifecycleOwner = viewLifecycleOwner
        viewModel = this@FeedFragment.viewModel
    }
    
    // binding.btnAdd je automaticky dostupné
    binding?.btnAdd?.setOnClickListener { ... }
    
    // Automatická aktualizácia UI (ak použiješ binding expressions)
    viewModel.feedItems.observe(this) { items ->
        // UI sa môže aktualizovať automaticky v XML
    }
}
```

---

## 🎯 **Kľúčové Komponenty:**

### **1. XML Layout s DataBinding:**

```xml
<layout xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Definuje premenné dostupné v layoute -->
    <data>
        <variable
            name="viewModel"
            type="com.example.mv11.FeedViewModel" />
    </data>
    
    <!-- Pôvodný layout -->
    <ConstraintLayout>
        <Button
            android:id="@+id/btnAdd"
            android:text="Pridať" />
        
        <!-- Môžeš používať binding expressions: -->
        <!-- android:text="@{viewModel.title}" -->
        <!-- android:onClick="@{() -> viewModel.addItem()}" -->
    </ConstraintLayout>
</layout>
```

---

### **2. Fragment s DataBinding:**

```kotlin
class FeedFragment : Fragment() {
    private var binding: FragmentFeedBinding? = null
    
    override fun onCreateView(...): View? {
        // Inflate layoutu pomocou DataBinding
        binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding?.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding?.apply {
            // Nastav lifecycleOwner (dôležité pre LiveData)
            lifecycleOwner = viewLifecycleOwner
            
            // Nastav ViewModel (dostupný v XML)
            this.viewModel = viewModel
            
            // Prístup k UI komponentom bez findViewById
            btnAdd.setOnClickListener { ... }
        }
    }
    
    override fun onDestroyView() {
        binding = null  // Dôležité pre memory management
        super.onDestroyView()
    }
}
```

---

## 🎓 **V React.js terminológii:**

### **DataBinding = React State + JSX**

**Kotlin (DataBinding):**
```kotlin
// XML
<data>
    <variable name="viewModel" type="FeedViewModel" />
</data>
<TextView android:text="@{viewModel.title}" />

// Kotlin
binding.viewModel = viewModel
```

**React:**
```jsx
// JSX
function FeedComponent() {
    const [title, setTitle] = useState("Feed");
    
    return <TextView text={title} />;
}
```

---

### **Binding Expressions = JSX Expressions**

**Kotlin (DataBinding):**
```xml
<!-- XML -->
<TextView
    android:text="@{viewModel.title}"
    android:visibility="@{viewModel.isLoading ? View.GONE : View.VISIBLE}"
    android:onClick="@{() -> viewModel.onButtonClick()}" />
```

**React:**
```jsx
// JSX
<TextView
    text={viewModel.title}
    visibility={viewModel.isLoading ? 'hidden' : 'visible'}
    onClick={() => viewModel.onButtonClick()} />
```

---

### **LiveData Observables = React State Updates**

**Kotlin (DataBinding + LiveData):**
```kotlin
// ViewModel
val title = MutableLiveData<String>()

// XML
<TextView android:text="@{viewModel.title}" />

// Automatická aktualizácia pri zmene title.value
```

**React:**
```jsx
// React State
const [title, setTitle] = useState("");

// JSX
<TextView text={title} />

// Automatická aktualizácia pri zmene title
```

---

### **findViewById = React Refs**

**Kotlin (Bez DataBinding):**
```kotlin
// ❌ findViewById
val button = view.findViewById<Button>(R.id.btnAdd)
button.setOnClickListener { ... }
```

**React:**
```jsx
// ❌ useRef
const buttonRef = useRef(null);
<Button ref={buttonRef} onClick={...} />
```

**Kotlin (S DataBinding):**
```kotlin
// ✅ Priamy prístup
binding?.btnAdd?.setOnClickListener { ... }
```

**React (S Hooks):**
```jsx
// ✅ Priamy prístup (v React nie je potrebné)
<Button onClick={...} />
```

---

## 📊 **Porovnanie:**

| Vlastnosť | Bez DataBinding | S DataBinding | React |
|-----------|----------------|--------------|-------|
| **Prístup k UI** | findViewById() | binding.btnAdd | Priamy |
| **Aktualizácia UI** | Manuálne | Automatická | Automatická |
| **Boilerplate** | Veľa | Menej | Žiadny |
| **Typová kontrola** | Runtime | Compile-time | Compile-time |
| **Binding expressions** | ❌ | ✅ | ✅ |

---

## ✅ **Výhody DataBinding:**

1. ✅ **Menej kódu**
   - Žiadne `findViewById()`
   - Menej boilerplate

2. ✅ **Automatická aktualizácia**
   - UI sa aktualizuje automaticky pri zmene dát
   - Ak používaš binding expressions v XML

3. ✅ **Typová kontrola**
   - Chyby sa zistia v compile-time
   - IDE autocomplete

4. ✅ **Čistejší kód**
   - Oddelenie UI logiky
   - Lepšia čitateľnosť

---

## ⚠️ **Dôležité poznámky:**

### **1. lifecycleOwner:**

```kotlin
binding?.lifecycleOwner = viewLifecycleOwner
```

**Prečo je dôležité:**
- Umožňuje DataBinding reagovať na lifecycle zmeny
- Automaticky zruší observables pri `onDestroy`
- Zabráni memory leaks

---

### **2. Null Safety:**

```kotlin
private var binding: FragmentFeedBinding? = null

override fun onDestroyView() {
    binding = null  // Dôležité!
    super.onDestroyView()
}
```

**Prečo:**
- Binding drží referenciu na View
- Ak nevyčistíš, môže dôjsť k memory leak
- Vždy nastav `binding = null` v `onDestroyView()`

---

### **3. Binding Expressions:**

```xml
<!-- ✅ DOBRÉ -->
<TextView android:text="@{viewModel.title}" />

<!-- ❌ ZLÉ (komplexná logika v XML) -->
<TextView android:text="@{viewModel.items.size > 0 ? viewModel.items[0].name : \"Empty\"}" />
```

**Best Practice:**
- Jednoduché výrazy v XML
- Komplexnú logiku do ViewModelu

---

## 🎯 **Príklady použitia:**

### **1. Jednoduchý binding:**

```xml
<TextView
    android:text="@{viewModel.title}"
    android:visibility="@{viewModel.isVisible ? View.VISIBLE : View.GONE}" />
```

---

### **2. Click listener:**

```xml
<Button
    android:onClick="@{() -> viewModel.onButtonClick()}" />
```

---

### **3. Two-way binding:**

```xml
<EditText
    android:text="@={viewModel.name}" />
```

**Poznámka:** `@=` namiesto `@{ }` pre two-way binding.

---

## 🚀 **Zhrnutie:**

**DataBinding = Automatická väzba medzi UI a dátami**

- **XML** = Definuje premenné a binding expressions
- **Binding Class** = Automaticky generovaná trieda
- **Fragment** = Používa binding namiesto findViewById
- **Výsledok** = Čistejší a jednoduchší kód

**V našej aplikácii:**
- ✅ FragmentFeedBinding automaticky generovaný
- ✅ Prístup k UI komponentom bez findViewById
- ✅ ViewModel dostupný v XML
- ✅ Automatická aktualizácia UI pri zmene dát

---

## 🔄 **Porovnanie s React:**

| Koncept | Android DataBinding | React |
|---------|---------------------|-------|
| **State Management** | LiveData + ViewModel | useState/useReducer |
| **UI Updates** | Automatické (LiveData) | Automatické (State) |
| **Binding** | XML expressions | JSX expressions |
| **Type Safety** | Compile-time | Compile-time (TypeScript) |
| **Boilerplate** | Stredné | Nízke |

---

**Máš teraz plne funkčný DataBinding!** 🎉

