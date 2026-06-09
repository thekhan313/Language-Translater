package com.speechlanguageconverter.utils

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class OcrHelper(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractText(uri: Uri, onProgress: ((String) -> Unit)? = null): String {
        onProgress?.invoke("Preparing image…")
        return try {
            val image = InputImage.fromFilePath(context, uri)
            onProgress?.invoke("Running OCR…")
            recognizeText(image)
        } catch (e: Exception) {
            onProgress?.invoke("OCR failed: ${e.message}")
            ""
        }
    }

    private suspend fun recognizeText(image: InputImage): String =
        suspendCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { continuation.resume(it.text) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }

    fun close() = recognizer.close()
}