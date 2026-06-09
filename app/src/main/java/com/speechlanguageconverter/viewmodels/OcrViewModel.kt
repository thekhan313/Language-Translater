package com.speechlanguageconverter.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.speechlanguageconverter.data.model.GrammarResult
import com.speechlanguageconverter.data.model.OcrResult
import com.speechlanguageconverter.data.repository.GrammarRepository
import com.speechlanguageconverter.data.repository.OcrRepository
import com.speechlanguageconverter.repository.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OcrViewModel(application: Application) : AndroidViewModel(application) {

    private val ocrRepository = OcrRepository(application)
    private val pdfRepository = PdfRepository(application)
    private val grammarRepository = GrammarRepository()

    val extractedText = MutableLiveData<String>()
    val correctedText = MutableLiveData<String>()
    val suggestions = MutableLiveData<String>()
    val isLoading = MutableLiveData<Boolean>()
    val statusMessage = MutableLiveData<String>()
    val showResultCard = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String>()

    fun runOcr(imageUri: Uri) {
        viewModelScope.launch {
            isLoading.value = true
            statusMessage.value = "Running OCR…"
            val result = ocrRepository.extractTextFromImage(imageUri) { progress ->
                statusMessage.postValue(progress)
            }
            isLoading.value = false
            when (result) {
                is OcrResult.Success -> {
                    extractedText.value = result.text
                    statusMessage.value = "OCR complete ✓ (${result.text.length} chars)"
                }
                is OcrResult.Empty -> statusMessage.value = "No text detected in image"
                is OcrResult.Error -> {
                    statusMessage.value = "OCR failed"
                    errorMessage.value = result.message
                }
            }
        }
    }

    fun extractFromPdf(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading.postValue(true)
            statusMessage.postValue("Opening PDF…")
            val result = pdfRepository.extractTextFromPdf(uri) { progress ->
                statusMessage.postValue(progress)
            }
            isLoading.postValue(false)
            when (result) {
                is OcrResult.Success -> {
                    extractedText.postValue(result.text)
                    statusMessage.postValue("PDF extracted ✓")
                }
                is OcrResult.Empty -> statusMessage.postValue("No text found in PDF")
                is OcrResult.Error -> {
                    statusMessage.postValue("PDF extraction failed")
                    errorMessage.postValue(result.message)
                }
            }
        }
    }

    fun checkGrammar(text: String) {
        if (text.isBlank()) {
            errorMessage.value = "Please enter or extract some text first"
            return
        }
        viewModelScope.launch {
            isLoading.value = true
            statusMessage.value = "Waking up grammar server…"
            when (val result = grammarRepository.checkGrammar(text)) {
                is GrammarResult.Success -> {
                    isLoading.value = false
                    correctedText.value = result.correctedText
                    suggestions.value = result.suggestions
                    showResultCard.value = true
                    statusMessage.value = "Check complete — ${result.matchCount} issue(s) found"
                }
                is GrammarResult.Error -> {
                    isLoading.value = false
                    statusMessage.value = result.message
                    errorMessage.value = result.message
                }
            }
        }
    }

    fun dismissResult() {
        showResultCard.value = false
        correctedText.value = ""
        suggestions.value = ""
        statusMessage.value = "Result dismissed"
    }

    override fun onCleared() {
        super.onCleared()
        ocrRepository.close()
    }
}