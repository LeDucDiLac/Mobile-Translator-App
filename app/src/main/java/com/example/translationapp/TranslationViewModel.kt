package com.example.translationapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class TranslationViewModel(private val historyDao: HistoryDao) : ViewModel() {

    // 1. Get all history (Converted from Flow to LiveData for the UI)
    val allHistory: LiveData<List<HistoryItem>> = historyDao.getAllHistory().asLiveData()

    // 2. Add a new translation
    fun addTranslation(original: String, translated: String) {
        val newItem = HistoryItem(
            originalText = original,
            translatedText = translated
        )
        // Launch a coroutine (run in background)
        viewModelScope.launch {
            historyDao.insert(newItem)
        }
    }
}

// 3. The Factory (Boilerplate code to help Android create the ViewModel)
class TranslationViewModelFactory(private val dao: HistoryDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TranslationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TranslationViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}