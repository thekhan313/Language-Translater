package com.speechlanguageconverter.data.model

sealed class OcrResult {
    data class Success(val text: String) : OcrResult()
    object Empty : OcrResult()
    data class Error(val message: String) : OcrResult()
}