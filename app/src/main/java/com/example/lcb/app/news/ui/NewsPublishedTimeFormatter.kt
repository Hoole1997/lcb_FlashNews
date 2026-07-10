package com.example.lcb.app.news.ui

import android.content.Context
import com.example.lcb.app.R
import com.example.lcb.news.model.NewsArticle
import java.text.SimpleDateFormat
import java.util.Date

/**
 * 发布时间在展示时动态计算，避免把“10 分钟前”写入缓存后逐渐失真。
 * 只有 RSS 明确提供时分时才使用相对时间；纯日期源保持日期展示。
 */
object NewsPublishedTimeFormatter {
    private const val MINUTE_MS = 60_000L
    private const val HOUR_MS = 60 * MINUTE_MS
    private const val RELATIVE_TIME_WINDOW_MS = 24 * HOUR_MS
    private const val FUTURE_TOLERANCE_MS = 2 * MINUTE_MS

    fun format(context: Context, article: NewsArticle, nowMillis: Long = System.currentTimeMillis()): String {
        val publishedAt = article.publishedAt
        if (publishedAt <= 0L) return article.publishedText
        if (!article.publishedText.contains(Regex("\\d{1,2}:\\d{2}"))) {
            return article.publishedText.ifBlank { absoluteTime(context, publishedAt) }
        }

        val elapsed = nowMillis - publishedAt
        return when {
            elapsed < -FUTURE_TOLERANCE_MS -> absoluteTime(context, publishedAt)
            elapsed < MINUTE_MS -> context.getString(R.string.flash_time_just_now)
            elapsed < HOUR_MS -> context.getString(R.string.flash_time_minutes_ago, elapsed / MINUTE_MS)
            elapsed < RELATIVE_TIME_WINDOW_MS -> context.getString(R.string.flash_time_hours_ago, elapsed / HOUR_MS)
            else -> absoluteTime(context, publishedAt)
        }
    }

    private fun absoluteTime(context: Context, epochMillis: Long): String {
        val locale = context.resources.configuration.locales[0]
        return SimpleDateFormat("yyyy-MM-dd HH:mm", locale).format(Date(epochMillis))
    }
}
