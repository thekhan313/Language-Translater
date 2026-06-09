package com.speechlanguageconverter.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.speechlanguageconverter.utils.LanguageUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Represents one row in the language list.
 */
data class LanguageItem(
    val name: String,
    val mlKitCode: String,
    val isSelected: Boolean,
    val downloadState: DownloadState
)

enum class DownloadState {
    DOWNLOADED,
    DOWNLOADING,
    NOT_DOWNLOADED
}

/**
 * A group shown in the RecyclerView (header + items).
 */
sealed class LanguageListItem {
    data class Header(val title: String) : LanguageListItem()
    data class Entry(val item: LanguageItem) : LanguageListItem()
}

class LanguageSelectViewModel : ViewModel() {

    private val modelManager = RemoteModelManager.getInstance()

    private var allLanguages: List<LanguageItem> = emptyList()
    private var currentSelected: String = ""
    private var searchQuery: String = ""

    private val _displayedLanguages = MutableLiveData<List<LanguageListItem>>()
    val displayedLanguages: LiveData<List<LanguageListItem>> = _displayedLanguages

    fun loadLanguages(selectedLanguage: String) {
        currentSelected = selectedLanguage
        viewModelScope.launch {
            val downloadedModels = try {
                modelManager.getDownloadedModels(TranslateRemoteModel::class.java).await()
                    .map { it.language }
                    .toSet()
            } catch (e: Exception) {
                emptySet()
            }

            allLanguages = LanguageUtils.supportedLanguages.map { lang ->
                LanguageItem(
                    name = lang.name,
                    mlKitCode = lang.mlKitCode,
                    isSelected = lang.name == selectedLanguage,
                    downloadState = if (downloadedModels.contains(lang.mlKitCode))
                        DownloadState.DOWNLOADED else DownloadState.NOT_DOWNLOADED
                )
            }
            applyFilter()
        }
    }

    fun filter(query: String) {
        searchQuery = query
        applyFilter()
    }

    fun downloadModel(languageName: String) {
        val lang = LanguageUtils.supportedLanguages.find { it.name == languageName } ?: return

        // Mark as downloading
        allLanguages = allLanguages.map {
            if (it.name == languageName) it.copy(downloadState = DownloadState.DOWNLOADING) else it
        }
        applyFilter()

        viewModelScope.launch {
            try {
                val model = TranslateRemoteModel.Builder(lang.mlKitCode).build()
                val conditions = DownloadConditions.Builder()
                    .requireWifi()
                    .build()
                modelManager.download(model, conditions).await()

                // Mark as downloaded
                allLanguages = allLanguages.map {
                    if (it.name == languageName) it.copy(downloadState = DownloadState.DOWNLOADED) else it
                }
            } catch (e: Exception) {
                // Revert to not downloaded on error
                allLanguages = allLanguages.map {
                    if (it.name == languageName) it.copy(downloadState = DownloadState.NOT_DOWNLOADED) else it
                }
            }
            applyFilter()
        }
    }

    private fun applyFilter() {
        val filtered = if (searchQuery.isBlank()) {
            allLanguages
        } else {
            allLanguages.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        val result = mutableListOf<LanguageListItem>()

        // "Recent" section: show currently selected language at top (only when not searching)
        if (searchQuery.isBlank()) {
            val recent = filtered.filter { it.isSelected }
            if (recent.isNotEmpty()) {
                result.add(LanguageListItem.Header("Recent languages"))
                recent.forEach { result.add(LanguageListItem.Entry(it)) }
            }
        }

        // "All languages" section
        result.add(LanguageListItem.Header("All languages"))
        filtered.forEach { result.add(LanguageListItem.Entry(it)) }

        _displayedLanguages.value = result
    }
}