package com.speechlanguageconverter.repository

import android.content.Context
import android.net.Uri
import com.speechlanguageconverter.data.model.OcrResult
import com.speechlanguageconverter.utils.PdfHelper

class PdfRepository(private val context: Context) {

    suspend fun extractTextFromPdf(
        uri: Uri,
        onProgress: ((String) -> Unit)? = null
    ): OcrResult {
        return try {
            val text = PdfHelper.extractText(context, uri, onProgress)
            if (text.isNotBlank()) OcrResult.Success(text.trim()) else OcrResult.Empty
        } catch (e: Exception) {
            OcrResult.Error(e.message ?: "Unknown PDF error")
        }
    }
}