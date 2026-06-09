package com.speechlanguageconverter.data.model

import com.google.gson.annotations.SerializedName

data class LanguageToolResponse(
    @SerializedName("matches") val matches: List<Match> = emptyList()
)

data class Match(
    @SerializedName("message") val message: String = "",
    @SerializedName("offset") val offset: Int = 0,
    @SerializedName("length") val length: Int = 0,
    @SerializedName("replacements") val replacements: List<Replacement> = emptyList()
)

data class Replacement(
    @SerializedName("value") val value: String = ""
)