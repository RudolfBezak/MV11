package com.example.mv11

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FeedViewModel : ViewModel() {

    private val _feedItems = MutableLiveData<List<MyItem>>()
    val feedItems: LiveData<List<MyItem>> get() = _feedItems

    private val _sampleString = MutableLiveData<String>()
    val sampleString: LiveData<String> get() = _sampleString

    /**
     * UserInput - MutableLiveData pre obojsmerný DataBinding.
     * 
     * Používa sa v EditText s @={viewModel.userInput} syntax.
     * Zmeny v EditText sa automaticky synchronizujú s týmto LiveData.
     */
    val userInput = MutableLiveData<String>()

    /**
     * DisplayText - LiveData pre jednosmerný DataBinding.
     * 
     * Používa sa v TextView s @{viewModel.displayText} syntax.
     * Zmeny v tomto LiveData sa automaticky zobrazia v TextView.
     */
    val displayText = MutableLiveData<String>()

    init {
        _feedItems.value = emptyList()
        _sampleString.value = ""
        userInput.value = ""  // Inicializácia pre obojsmerný binding
        displayText.value = "Zadajte text..."  // Inicializácia pre jednosmerný binding
    }

    fun updateItems(items: List<MyItem>) {
        _feedItems.value = items
    }

    fun addItem(item: MyItem) {
        val currentItems = _feedItems.value?.toMutableList() ?: mutableListOf()
        currentItems.add(0, item)
        _feedItems.value = currentItems
    }

    fun removeFirstItem() {
        val currentItems = _feedItems.value?.toMutableList() ?: return
        if (currentItems.isNotEmpty()) {
            currentItems.removeAt(0)
            _feedItems.value = currentItems
        }
    }

    fun changeFirstItem() {
        val currentItems = _feedItems.value?.toMutableList() ?: return
        if (currentItems.isNotEmpty()) {
            val updatedItem = currentItems[0].copy(text = "Zmenený text! (${System.currentTimeMillis()})")
            currentItems[0] = updatedItem
            _feedItems.value = currentItems
        }
    }

    fun changeRangeOfItems() {
        val currentItems = _feedItems.value?.toMutableList() ?: return
        if (currentItems.size >= 2) {
            currentItems[0] = currentItems[0].copy(text = "Zmenený rozsah 1")
            currentItems[1] = currentItems[1].copy(text = "Zmenený rozsah 2")
            _feedItems.value = currentItems
        }
    }

    fun updateString(value: String) {
        _sampleString.value = value
    }

    /**
     * Metódy pre click eventy v XML.
     * Tieto metódy sa môžu volať priamo z XML pomocou:
     * android:onClick="@{() -> viewModel.onAddButtonClick()}"
     */

    fun onAddButtonClick() {
        val newItem = MyItem(
            System.currentTimeMillis().toInt(),
            R.drawable.file_foreground,
            "Nová položka z XML"
        )
        addItem(newItem)
    }

    fun onRemoveButtonClick() {
        removeFirstItem()
    }

    fun onChangeButtonClick() {
        changeFirstItem()
    }

    fun onChangeRangeButtonClick() {
        changeRangeOfItems()
    }

    fun onResetButtonClick() {
        val initialItems = listOf(
            MyItem(1, R.drawable.file_foreground, "Prvý"),
            MyItem(2, R.drawable.map_foreground, "Druhý"),
            MyItem(3, R.drawable.profile_foreground, "Tretí"),
        )
        updateItems(initialItems)
    }

    /**
     * Aktualizuje displayText na základe userInput.
     * Toto demonštruje ako obojsmerný binding môže ovplyvniť jednosmerný binding.
     */
    fun onUserInputChanged() {
        displayText.value = "Zadali ste: ${userInput.value ?: ""}"
    }
}


