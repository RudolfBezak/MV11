package com.example.mv11

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FeedViewModel : ViewModel() {

    private val _feedItems = MutableLiveData<List<MyItem>>()
    val feedItems: LiveData<List<MyItem>> get() = _feedItems

    private val _sampleString = MutableLiveData<String>()
    val sampleString: LiveData<String> get() = _sampleString

    init {
        _feedItems.value = emptyList()
        _sampleString.value = ""
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
}


