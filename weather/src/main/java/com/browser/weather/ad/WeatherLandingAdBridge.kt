package com.browser.weather.ad

import androidx.appcompat.app.AppCompatActivity

/**
 * 天气模块只暴露页面落地事件，不直接依赖宿主 App 的广告实现。
 */
fun interface WeatherLandingAdHandler {
    fun onWeatherPageLanded(activity: AppCompatActivity)
}

object WeatherLandingAdBridge {
    @Volatile
    private var handler: WeatherLandingAdHandler? = null

    fun setHandler(handler: WeatherLandingAdHandler?) {
        this.handler = handler
    }

    fun notifyPageLanded(activity: AppCompatActivity) {
        handler?.onWeatherPageLanded(activity)
    }
}
