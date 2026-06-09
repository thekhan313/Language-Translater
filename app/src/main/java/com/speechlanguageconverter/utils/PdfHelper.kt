package com.speechlanguageconverter.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object PdfHelper {

    suspend fun extractText(
        context: Context,
        uri: Uri,
        onProgress: ((String) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val stringBuilder = StringBuilder()

        try {
            // Open PDF via ParcelFileDescriptor
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return@withContext "Could not open PDF"

            val renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            onProgress?.invoke("PDF has $pageCount page(s), starting OCR…")

            for (i in 0 until pageCount) {
                onProgress?.invoke("Processing page ${i + 1} of $pageCount…")

                val page = renderer.openPage(i)

                // Render at 2x scale for better OCR accuracy
                val width = page.width * 2
                val height = page.height * 2

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                // Fill white background (PDF pages are transparent by default)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                // Run ML Kit OCR on the rendered bitmap
                val pageText = recognizeBitmap(recognizer, bitmap)
                bitmap.recycle()

                if (pageText.isNotBlank()) {
                    if (stringBuilder.isNotEmpty()) stringBuilder.append("\n\n--- Page ${i + 1} ---\n\n")
                    stringBuilder.append(pageText.trim())
                }
            }

            renderer.close()
            pfd.close()

        } catch (e: Exception) {
            return@withContext "Error reading PDF: ${e.message}"
        } finally {
            recognizer.close()
        }

        if (stringBuilder.isEmpty()) "" else stringBuilder.toString()
    }

    private suspend fun recognizeBitmap(
        recognizer: com.google.mlkit.vision.text.TextRecognizer,
        bitmap: Bitmap
    ): String = suspendCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result -> continuation.resume(result.text) }
            .addOnFailureListener { e -> continuation.resumeWithException(e) }
    }
}