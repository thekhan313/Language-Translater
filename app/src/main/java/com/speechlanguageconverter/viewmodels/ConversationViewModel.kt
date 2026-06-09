package com.speechlanguageconverter.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speechlanguageconverter.data.model.TranslationResult
import com.speechlanguageconverter.data.repository.TranslationRepository
import com.speechlanguageconverter.utils.LanguageUtils
import kotlinx.coroutines.launch

class ConversationViewModel : ViewModel() {

    private val repository = TranslationRepository()

    private val _conversations = MutableLiveData<MutableList<TranslationResult>>(mutableListOf())
    val conversations: LiveData<MutableList<TranslationResult>> = _conversations

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    var sourceLanguage = "English"
    var targetLanguage = "Urdu"
    var sourceLanguageName: String = "English"
    var targetLanguageName: String = "French"   // or "Urdu" for conversation

    fun translateAndAdd(spokenText: String, sourceLang: String, targetLang: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val sourceMlKit = LanguageUtils.getMlKitCode(sourceLang)
                val targetMlKit = LanguageUtils.getMlKitCode(targetLang)

                val translated = repository.translate(spokenText, sourceMlKit, targetMlKit)

                val result = TranslationResult(
                    originalText = spokenText,
                    translatedText = translated,
                    sourceLanguage = sourceLang,
                    targetLanguage = targetLang
                )

                val list = _conversations.value ?: mutableListOf()
                list.add(result)
                _conversations.value = list
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() { _error.value = null }

}