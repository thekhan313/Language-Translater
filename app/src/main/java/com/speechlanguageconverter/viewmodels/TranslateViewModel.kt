package com.speechlanguageconverter.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speechlanguageconverter.data.repository.TranslationRepository
import com.speechlanguageconverter.utils.LanguageUtils
import kotlinx.coroutines.launch

class TranslateViewModel : ViewModel() {

    private val repository = TranslationRepository()

    private val _translatedText = MutableLiveData<String>()
    val translatedText: LiveData<String> = _translatedText

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    var sourceLanguageName: String = "English"
    var targetLanguageName: String = "French"   // or "Urdu" for conversation

    fun translate(text: String, sourceLang: String, targetLang: String) {
        if (text.isBlank()) return
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.translate(
                    text,
                    LanguageUtils.getMlKitCode(sourceLang),
                    LanguageUtils.getMlKitCode(targetLang)
                )
                _translatedText.value = result
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() { _error.value = null }
}