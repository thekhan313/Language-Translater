package com.speechlanguageconverter.data.repository

import com.speechlanguageconverter.data.model.GrammarResult
import com.speechlanguageconverter.data.remote.LanguageToolClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GrammarRepository {

    suspend fun checkGrammar(text: String): GrammarResult {
        return try {
            withContext(Dispatchers.IO) { LanguageToolClient.pingServer() }

            val response = LanguageToolClient.api.checkText(text = text, language = "en-US")

            var corrected = text
            val sortedMatches = response.matches.sortedByDescending { it.offset }
            val suggestionsList = mutableListOf<String>()

            for (match in sortedMatches) {
                val replacement = match.replacements.firstOrNull()?.value ?: continue
                val start = match.offset
                val end = match.offset + match.length
                if (start >= 0 && end <= corrected.length) {
                    corrected = corrected.substring(0, start) + replacement + corrected.substring(end)
                }
                suggestionsList.add("• ${match.message} → \"$replacement\"")
            }

            val suggestionsText = if (suggestionsList.isNotEmpty())
                "Suggestions:\n" + suggestionsList.take(10).joinToString("\n")
            else "No issues found!"

            GrammarResult.Success(
                correctedText = corrected,
                suggestions = suggestionsText,
                matchCount = response.matches.size
            )
        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "Server timed out — try again in 30 seconds"
                e.message?.contains("Unable to resolve host") == true ->
                    "No internet connection"
                else -> "Error: ${e.message}"
            }
            GrammarResult.Error(errorMsg)
        }
    }
}