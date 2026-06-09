package com.speechlanguageconverter.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.speechlanguageconverter.R
import com.speechlanguageconverter.databinding.ItemLanguageHeaderBinding
import com.speechlanguageconverter.databinding.ItemLanguageEntryBinding
import com.speechlanguageconverter.viewmodels.DownloadState
import com.speechlanguageconverter.viewmodels.LanguageItem
import com.speechlanguageconverter.viewmodels.LanguageListItem

class LanguageSelectAdapter(
    private val onLanguageSelected: (String) -> Unit,
    private val onDownloadClicked: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ENTRY = 1
    }

    private val items = mutableListOf<LanguageListItem>()

    fun submitGroups(list: List<LanguageListItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is LanguageListItem.Header -> TYPE_HEADER
        is LanguageListItem.Entry -> TYPE_ENTRY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemLanguageHeaderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                HeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemLanguageEntryBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                EntryViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is LanguageListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is LanguageListItem.Entry -> (holder as EntryViewHolder).bind(item.item)
        }
    }

    override fun getItemCount() = items.size

    inner class HeaderViewHolder(private val binding: ItemLanguageHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(header: LanguageListItem.Header) {
            binding.tvHeader.text = header.title
        }
    }

    inner class EntryViewHolder(private val binding: ItemLanguageEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LanguageItem) {
            binding.tvLanguageName.text = item.name

            // Left checkmark: visible only when this language is selected
            binding.ivSelected.visibility = if (item.isSelected) View.VISIBLE else View.INVISIBLE

            // Right icon: depends on download state
            when (item.downloadState) {
                DownloadState.DOWNLOADED -> {
                    binding.ivDownloadStatus.setImageResource(R.drawable.ic_check_downloaded)
                    binding.ivDownloadStatus.isEnabled = false
                    binding.progressDownload.visibility = View.GONE
                    binding.ivDownloadStatus.visibility = View.VISIBLE
                }
                DownloadState.NOT_DOWNLOADED -> {
                    binding.ivDownloadStatus.setImageResource(R.drawable.ic_download)
                    binding.ivDownloadStatus.isEnabled = true
                    binding.progressDownload.visibility = View.GONE
                    binding.ivDownloadStatus.visibility = View.VISIBLE
                    binding.ivDownloadStatus.setOnClickListener {
                        onDownloadClicked(item.name)
                    }
                }
                DownloadState.DOWNLOADING -> {
                    binding.ivDownloadStatus.visibility = View.GONE
                    binding.progressDownload.visibility = View.VISIBLE
                }
            }

            // Tap the row to select the language
            binding.root.setOnClickListener {
                onLanguageSelected(item.name)
            }
        }
    }
}