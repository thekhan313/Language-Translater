package com.speechlanguageconverter.data.model

sealed class GrammarResult {
    data class Success(
        val correctedText: String,
        val suggestions: String,
        val matchCount: Int
    ) : GrammarResult()
    data class Error(val message: String) : GrammarResult()
}