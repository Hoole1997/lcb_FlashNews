package com.example.lcb.app.news.ad

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.example.lcb.app.utils.loadInterstitial

/**
 * 新闻详情插屏展示闸口。
 * 详情页先完成跳转，再由这里按阅读次数和时间间隔决定是否展示，避免破坏点击跳转体验。
 */
object NewsInterstitialGate {
    private const val PREFS_NAME = "flash_news_interstitial_gate"
    private const val KEY_NEWS_SINCE_LAST_AD = "news_since_last_ad"
    private const val KEY_LAST_SHOWN_AT = "last_shown_at"
    private const val KEY_LAST_ATTEMPT_AT = "last_attempt_at"
    private const val KEY_HAS_ATTEMPTED_FIRST_AD = "has_attempted_first_ad"

    private const val MIN_NEWS_BETWEEN_ADS = 3
    private const val MIN_TIME_BETWEEN_ADS_MS = 90_000L
    private const val MIN_RETRY_INTERVAL_MS = 30_000L

    fun maybeShowAfterLanding(activity: FragmentActivity) {
        if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        if (!markNewsOpenedAndShouldAttempt(activity)) return

        activity.loadInterstitial { shown ->
            if (shown) {
                recordShown(activity)
            }
        }
    }

    private fun markNewsOpenedAndShouldAttempt(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val newsSinceLastAd = prefs.getInt(KEY_NEWS_SINCE_LAST_AD, 0) + 1
        val lastShownAt = prefs.getLong(KEY_LAST_SHOWN_AT, 0L)
        val lastAttemptAt = prefs.getLong(KEY_LAST_ATTEMPT_AT, 0L)

        prefs.edit()
            .putInt(KEY_NEWS_SINCE_LAST_AD, newsSinceLastAd)
            .apply()

        // 首次进入新闻详情落地后直接尝试展示；之后再进入频控规则。
        if (!prefs.getBoolean(KEY_HAS_ATTEMPTED_FIRST_AD, false)) {
            prefs.edit()
                .putBoolean(KEY_HAS_ATTEMPTED_FIRST_AD, true)
                .putLong(KEY_LAST_ATTEMPT_AT, now)
                .apply()
            return true
        }

        val hasEnoughNews = newsSinceLastAd >= MIN_NEWS_BETWEEN_ADS
        val hasEnoughTimeSinceShown = lastShownAt == 0L || now - lastShownAt >= MIN_TIME_BETWEEN_ADS_MS
        val hasEnoughTimeSinceAttempt = lastAttemptAt == 0L || now - lastAttemptAt >= MIN_RETRY_INTERVAL_MS
        if (!hasEnoughNews || !hasEnoughTimeSinceShown || !hasEnoughTimeSinceAttempt) {
            return false
        }

        prefs.edit()
            .putLong(KEY_LAST_ATTEMPT_AT, now)
            .apply()
        return true
    }

    private fun recordShown(context: Context) {
        val now = System.currentTimeMillis()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_NEWS_SINCE_LAST_AD, 0)
            .putLong(KEY_LAST_SHOWN_AT, now)
            .putLong(KEY_LAST_ATTEMPT_AT, now)
            .apply()
    }
}
