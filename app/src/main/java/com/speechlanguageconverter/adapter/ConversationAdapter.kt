package com.speechlanguageconverter.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.speechlanguageconverter.data.model.TranslationResult
import com.speechlanguageconverter.databinding.ItemConversationBinding

class ConversationAdapter(
    private val onSpeak: (TranslationResult) -> Unit,
    private val onCopy: (TranslationResult) -> Unit,
    private val onShare: (TranslationResult) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {

    private val items = mutableListOf<TranslationResult>()

    fun submitList(list: List<TranslationResult>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemConversationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TranslationResult, position: Int) {
            binding.tvOriginal.text = item.originalText
            binding.tvTranslated.text = item.translatedText
            binding.tvSourceLang.text = item.sourceLanguage.take(2).uppercase()
            binding.tvTargetLang.text = item.targetLanguage.take(2).uppercase()
            binding.btnSpeak.setOnClickListener { onSpeak(item) }
            binding.btnCopy.setOnClickListener { onCopy(item) }
            binding.btnShare.setOnClickListener { onShare(item) }
            binding.btnDelete.setOnClickListener { onDelete(position) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemConversationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position], position)

    override fun getItemCount() = items.size

    fun removeAt(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }
}