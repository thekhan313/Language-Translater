package com.speechlanguageconverter.utils


import com.google.mlkit.nl.translate.TranslateLanguage
import java.util.Locale

object LanguageUtils {

    data class Language(val name: String, val code: String, val mlKitCode: String)

    val supportedLanguages = listOf(
        Language("Afrikaans", "af", TranslateLanguage.AFRIKAANS),
        Language("Albanian", "sq", TranslateLanguage.ALBANIAN),
        Language("Arabic", "ar", TranslateLanguage.ARABIC),
        Language("Belarusian", "be", TranslateLanguage.BELARUSIAN),
        Language("Bengali", "bn", TranslateLanguage.BENGALI),
        Language("Bulgarian", "bg", TranslateLanguage.BULGARIAN),
        Language("Catalan", "ca", TranslateLanguage.CATALAN),
        Language("Chinese", "zh", TranslateLanguage.CHINESE),
        Language("Croatian", "hr", TranslateLanguage.CROATIAN),
        Language("Czech", "cs", TranslateLanguage.CZECH),
        Language("Danish", "da", TranslateLanguage.DANISH),
        Language("Dutch", "nl", TranslateLanguage.DUTCH),
        Language("English", "en", TranslateLanguage.ENGLISH),
        Language("Esperanto", "eo", TranslateLanguage.ESPERANTO),
        Language("Estonian", "et", TranslateLanguage.ESTONIAN),
        Language("Finnish", "fi", TranslateLanguage.FINNISH),
        Language("French", "fr", TranslateLanguage.FRENCH),
        Language("Galician", "gl", TranslateLanguage.GALICIAN),
        Language("Georgian", "ka", TranslateLanguage.GEORGIAN),
        Language("German", "de", TranslateLanguage.GERMAN),
        Language("Greek", "el", TranslateLanguage.GREEK),
        Language("Gujarati", "gu", TranslateLanguage.GUJARATI),
        Language("Haitian Creole", "ht", TranslateLanguage.HAITIAN_CREOLE),
        Language("Hebrew", "he", TranslateLanguage.HEBREW),
        Language("Hindi", "hi", TranslateLanguage.HINDI),
        Language("Hungarian", "hu", TranslateLanguage.HUNGARIAN),
        Language("Icelandic", "is", TranslateLanguage.ICELANDIC),
        Language("Indonesian", "id", TranslateLanguage.INDONESIAN),
        Language("Irish", "ga", TranslateLanguage.IRISH),
        Language("Italian", "it", TranslateLanguage.ITALIAN),
        Language("Japanese", "ja", TranslateLanguage.JAPANESE),
        Language("Kannada", "kn", TranslateLanguage.KANNADA),
        Language("Korean", "ko", TranslateLanguage.KOREAN),
        Language("Latvian", "lv", TranslateLanguage.LATVIAN),
        Language("Lithuanian", "lt", TranslateLanguage.LITHUANIAN),
        Language("Macedonian", "mk", TranslateLanguage.MACEDONIAN),
        Language("Malay", "ms", TranslateLanguage.MALAY),
        Language("Maltese", "mt", TranslateLanguage.MALTESE),
        Language("Marathi", "mr", TranslateLanguage.MARATHI),
        Language("Norwegian", "no", TranslateLanguage.NORWEGIAN),
        Language("Persian", "fa", TranslateLanguage.PERSIAN),
        Language("Polish", "pl", TranslateLanguage.POLISH),
        Language("Portuguese", "pt", TranslateLanguage.PORTUGUESE),
        Language("Romanian", "ro", TranslateLanguage.ROMANIAN),
        Language("Russian", "ru", TranslateLanguage.RUSSIAN),
        Language("Slovak", "sk", TranslateLanguage.SLOVAK),
        Language("Slovenian", "sl", TranslateLanguage.SLOVENIAN),
        Language("Spanish", "es", TranslateLanguage.SPANISH),
        Language("Swahili", "sw", TranslateLanguage.SWAHILI),
        Language("Swedish", "sv", TranslateLanguage.SWEDISH),
        Language("Tagalog", "tl", TranslateLanguage.TAGALOG),
        Language("Tamil", "ta", TranslateLanguage.TAMIL),
        Language("Telugu", "te", TranslateLanguage.TELUGU),
        Language("Thai", "th", TranslateLanguage.THAI),
        Language("Turkish", "tr", TranslateLanguage.TURKISH),
        Language("Ukrainian", "uk", TranslateLanguage.UKRAINIAN),
        Language("Urdu", "ur", TranslateLanguage.URDU),
        Language("Vietnamese", "vi", TranslateLanguage.VIETNAMESE),
        Language("Welsh", "cy", TranslateLanguage.WELSH)
    )
    fun getLanguageNames(): Array<String> = supportedLanguages.map { it.name }.toTypedArray()

    fun getMlKitCode(languageName: String): String =
        supportedLanguages.find { it.name == languageName }?.mlKitCode ?: TranslateLanguage.ENGLISH

    fun getBcp47Code(languageName: String): String =
        supportedLanguages.find { it.name == languageName }?.code ?: "en"

    fun getLocale(languageName: String): Locale =
        Locale(getBcp47Code(languageName))
}