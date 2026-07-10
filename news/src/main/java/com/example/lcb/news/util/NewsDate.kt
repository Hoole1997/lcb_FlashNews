package com.example.lcb.news.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal object NewsDate {
    private val formats = listOf(
        DateFormat("EEE, dd MMM yyyy HH:mm:ss Z"),
        DateFormat("EEE, d MMM yyyy HH:mm:ss Z"),
        DateFormat("EEE, dd MMM yyyy HH:mm:ss zzz"),
        DateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", usesUtc = true),
        DateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", usesUtc = true),
        DateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"),
        DateFormat("yyyy-MM-dd'T'HH:mmXXX"),
        DateFormat("yyyy-MM-dd HH:mm:ss Z"),
        DateFormat("yyyy-MM-dd HH:mm:ss"),
        DateFormat("yyyy-MM-dd"),
    )

    fun parse(raw: String): Long {
        val value = raw.trim()
        if (value.isBlank()) return 0L
        for (format in formats) {
            val parsed = runCatching {
                SimpleDateFormat(format.pattern, Locale.US).apply {
                    isLenient = false
                    if (format.usesUtc) timeZone = TimeZone.getTimeZone("UTC")
                }.parse(value)?.time
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return 0L
    }

    fun display(epochMillis: Long, fallback: String): String {
        if (epochMillis <= 0L) return fallback.takeIf { it.isNotBlank() } ?: ""
        val pattern = if (fallback.hasTimeComponent()) "yyyy-MM-dd HH:mm" else "yyyy-MM-dd"
        return SimpleDateFormat(pattern, Locale.US).format(epochMillis)
    }

    private fun String.hasTimeComponent(): Boolean {
        return contains(Regex("(?:T|\\s)\\d{1,2}:\\d{2}")) ||
            contains(Regex("\\d{1,2}:\\d{2}:\\d{2}"))
    }

    private data class DateFormat(
        val pattern: String,
        val usesUtc: Boolean = false,
    )
}
