package com.example.lcb.news.config

/**
 * News SDK 支持的语言配置键。语言解析集中在 SDK 内，避免 UI 层拼接资源文件名或远程配置键。
 */
internal object NewsLanguage {
    const val DEFAULT = "en"

    private val supported = setOf(
        "en", "es", "fr", "de", "pt", "ru", "hi", "id", "ja", "ko", "zh-CN",
    )

    fun fromLocaleTag(localeTag: String?): String {
        val normalized = localeTag.orEmpty().replace('_', '-').trim()
        if (normalized.isBlank()) return DEFAULT
        if (normalized.substringBefore('-').equals("zh", ignoreCase = true)) {
            return "zh-CN"
        }
        val language = normalized.substringBefore('-').lowercase()
        return supported.firstOrNull { it.equals(language, ignoreCase = true) } ?: DEFAULT
    }

    fun localizedAssetName(languageKey: String): String {
        return "news_feeds_${languageKey.lowercase().replace('-', '_')}.json"
    }

    fun remoteConfigKey(baseKey: String, languageKey: String): String {
        return if (languageKey == DEFAULT) {
            baseKey
        } else {
            "${baseKey}_${languageKey.lowercase().replace('-', '_')}"
        }
    }
}
