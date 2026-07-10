package com.example.lcb.app.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppLanguageManager {
    fun selectedLanguage(): AppLanguage {
        return AppLanguage.fromLocaleTag(currentLocaleTag())
    }

    fun selectLanguage(language: AppLanguage): Boolean {
        val oldTag = currentLocaleTag()
        val newTag = language.localeTag?.takeIf { it.isNotBlank() }
        AppCompatDelegate.setApplicationLocales(language.toLocaleListCompat())
        return oldTag.orEmpty() != newTag.orEmpty()
    }

    private fun currentLocaleTag(): String? {
        return AppCompatDelegate.getApplicationLocales()
            .toLanguageTags()
            .takeIf { it.isNotBlank() }
    }

    private fun AppLanguage.toLocaleListCompat(): LocaleListCompat {
        val normalized = localeTag?.takeIf { it.isNotBlank() }
        return if (normalized == null) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(normalized)
        }
    }
}
