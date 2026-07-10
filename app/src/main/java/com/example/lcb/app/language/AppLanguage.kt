package com.example.lcb.app.language

import android.content.Context
import com.example.lcb.app.R

data class AppLanguage(
    val key: String,
    val localeTag: String?,
    val displayName: String,
) {
    val isFollowSystem: Boolean get() = key == KEY_FOLLOW_SYSTEM

    fun label(context: Context): String {
        // 语言名称保持原语种展示；“跟随系统”属于功能项，需要跟随界面语言变化。
        return if (isFollowSystem) {
            context.getString(R.string.flash_setting_language_default)
        } else {
            displayName
        }
    }

    companion object {
        const val KEY_FOLLOW_SYSTEM = "system"

        val supportedLanguages: List<AppLanguage> = listOf(
            AppLanguage(KEY_FOLLOW_SYSTEM, null, ""),
            AppLanguage("en", "en", "English"),
            AppLanguage("es", "es", "Español"),
            AppLanguage("fr", "fr", "Français"),
            AppLanguage("de", "de", "Deutsch"),
            AppLanguage("pt", "pt", "Português"),
            AppLanguage("ru", "ru", "Русский"),
            AppLanguage("hi", "hi", "हिन्दी"),
            AppLanguage("id", "id", "Bahasa Indonesia"),
            AppLanguage("ja", "ja", "日本語"),
            AppLanguage("ko", "ko", "한국어"),
            AppLanguage("zh-CN", "zh-CN", "简体中文"),
        )

        fun fromKey(key: String?): AppLanguage {
            return supportedLanguages.firstOrNull { it.key == key }
                ?: supportedLanguages.first()
        }

        fun fromLocaleTag(localeTag: String?): AppLanguage {
            val normalized = localeTag
                ?.substringBefore(',')
                ?.replace('_', '-')
                ?.takeIf { it.isNotBlank() }
                ?: return supportedLanguages.first()
            return supportedLanguages.firstOrNull { language ->
                language.localeTag.equals(normalized, ignoreCase = true)
            } ?: supportedLanguages.firstOrNull { language ->
                val languageCode = language.localeTag?.substringBefore('-')
                languageCode.equals(normalized.substringBefore('-'), ignoreCase = true)
            } ?: supportedLanguages.first()
        }
    }
}
