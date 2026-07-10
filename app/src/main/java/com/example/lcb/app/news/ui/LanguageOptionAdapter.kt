package com.example.lcb.app.news.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lcb.app.R
import com.example.lcb.app.databinding.ItemFlashLanguageOptionBinding
import com.example.lcb.app.language.AppLanguage

class LanguageOptionAdapter(
    private val onLanguageClick: (AppLanguage) -> Unit,
) : RecyclerView.Adapter<LanguageOptionAdapter.LanguageViewHolder>() {
    private val languages = AppLanguage.supportedLanguages
    private var selectedKey: String = AppLanguage.KEY_FOLLOW_SYSTEM

    fun submitSelectedKey(key: String) {
        selectedKey = key
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemFlashLanguageOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(languages[position], languages[position].key == selectedKey)
    }

    override fun getItemCount(): Int = languages.size

    inner class LanguageViewHolder(
        private val binding: ItemFlashLanguageOptionBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(language: AppLanguage, selected: Boolean) {
            binding.languageName.text = language.label(binding.root.context)
            binding.languageName.setTextColor(
                binding.root.context.getColor(
                    if (selected) R.color.flash_red else R.color.flash_text_primary,
                ),
            )
            binding.languageName.typeface = if (selected) {
                android.graphics.Typeface.DEFAULT_BOLD
            } else {
                android.graphics.Typeface.DEFAULT
            }
            binding.languageCheck.visibility = if (selected) View.VISIBLE else View.GONE
            binding.root.setBackgroundResource(
                if (selected) R.drawable.bg_flash_language_option_selected else 0,
            )
            binding.root.setOnClickListener { onLanguageClick(language) }
        }
    }
}
