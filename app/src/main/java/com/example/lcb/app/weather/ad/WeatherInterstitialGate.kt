package com.example.lcb.app.weather.ad

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.browser.weather.ad.WeatherLandingAdBridge
import com.browser.weather.ad.WeatherLandingAdHandler
import com.example.lcb.app.utils.loadInterstitial

/**
 * 天气页插屏展示闸口。
 * 天气页面先完成跳转和 Compose 首屏挂载，再延迟触发插屏，避免影响页面进入体验。
 */
object WeatherInterstitialGate {
    private const val SHOW_INTERSTITIAL_AFTER_LANDING_MS = 450L

    fun install() {
        WeatherLandingAdBridge.setHandler(
            WeatherLandingAdHandler { activity ->
                maybeShowAfterLanding(activity)
            },
        )
    }

    private fun maybeShowAfterLanding(activity: AppCompatActivity) {
        activity.window.decorView.postDelayed(
            {
                if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@postDelayed
                if (activity.isFinishing || activity.isDestroyed) return@postDelayed
                activity.loadInterstitial { }
            },
            SHOW_INTERSTITIAL_AFTER_LANDING_MS,
        )
    }
}
