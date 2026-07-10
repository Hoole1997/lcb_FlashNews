package com.example.lcb.news.util

import android.text.Html
import java.net.URLDecoder

internal object NewsText {
    private val tagRegex = Regex("<[^>]+>")
    private val cdataRegex = Regex("<!\\[CDATA\\[(.*?)]]>", RegexOption.DOT_MATCHES_ALL)

    fun clean(raw: String): String {
        if (raw.isBlank()) return ""
        val withoutCdata = cdataRegex.replace(raw) { it.groupValues[1] }
        val decoded = Html.fromHtml(withoutCdata, Html.FROM_HTML_MODE_LEGACY).toString()
        return tagRegex.replace(decoded, "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun truncateSummary(value: String, maxLength: Int = 300): String {
        val clean = clean(value)
        if (clean.length <= maxLength) return clean
        return clean.take(maxLength).substringBeforeLast(' ').ifBlank { clean.take(maxLength) } + "..."
    }

    /**
     * RSS 常同时提供 description、summary 和 content:encoded。部分 description
     * 只有图片标签，因此必须以清洗后的文字是否有效为准，而不是以原始 XML 是否为空为准。
     */
    fun firstUsableSummary(values: Iterable<String>, maxLength: Int = 300): String {
        return values.firstNotNullOfOrNull { value ->
            truncateSummary(value, maxLength).takeIf { it.isNotBlank() }
        }.orEmpty()
    }

    fun decodeUrl(value: String): String {
        return runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
    }
}
