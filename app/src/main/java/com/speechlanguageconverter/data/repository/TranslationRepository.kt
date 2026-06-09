package com.speechlanguageconverter.data.repository

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TranslationRepository {

    suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): String = suspendCancellableCoroutine { continuation ->
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguage)
            .setTargetLanguage(targetLanguage)
            .build()

        val translator = Translation.getClient(options)

        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { result ->
                        continuation.resume(result)
                        translator.close()
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                        translator.close()
                    }
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
                translator.close()
            }
    }
}
