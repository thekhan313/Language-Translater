package com.speechlanguageconverter.data.repository

import android.content.Context
import android.net.Uri
import com.speechlanguageconverter.data.model.OcrResult
import com.speechlanguageconverter.utils.OcrHelper

class OcrRepository(context: Context) {
    private val ocrHelper = OcrHelper(context)

    suspend fun extractTextFromImage(uri: Uri, onProgress: ((String) -> Unit)? = null): OcrResult {
        return try {
            val text = ocrHelper.extractText(uri, onProgress)
            if (text.isNotBlank()) OcrResult.Success(text.trim()) else OcrResult.Empty
        } catch (e: Exception) {
            OcrResult.Error(e.message ?: "Unknown OCR error")
        }
    }

    fun close() = ocrHelper.close()
}