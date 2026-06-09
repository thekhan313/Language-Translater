package com.speechlanguageconverter.data.remote

import com.speechlanguageconverter.data.model.LanguageToolResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface LanguageToolApi {
    @FormUrlEncoded
    @POST("v2/check")
    suspend fun checkText(
        @Field("text") text: String,
        @Field("language") language: String
    ): LanguageToolResponse
}