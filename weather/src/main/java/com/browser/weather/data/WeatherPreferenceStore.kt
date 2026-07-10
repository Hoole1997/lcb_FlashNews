package com.browser.weather.data

import android.content.Context
import com.google.gson.Gson

/**
 * 保存用户主动选择的城市和最近一次成功天气。
 * IP 定位结果不会覆盖手选城市；缓存天气用于首页立即同步展示和离线兜底。
 */
internal class WeatherPreferenceStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun selectedLocation(): SavedWeatherLocation? {
        val raw = preferences.getString(KEY_SELECTED_LOCATION, null) ?: return null
        return runCatching { gson.fromJson(raw, SavedWeatherLocation::class.java) }.getOrNull()
    }

    fun saveSelectedLocation(location: SavedWeatherLocation) {
        preferences.edit().putString(KEY_SELECTED_LOCATION, gson.toJson(location)).apply()
    }

    fun latestWeather(): WeatherData? {
        val raw = preferences.getString(KEY_LATEST_WEATHER, null) ?: return null
        return runCatching { gson.fromJson(raw, WeatherData::class.java) }.getOrNull()
    }

    fun saveLatestWeather(weather: WeatherData) {
        preferences.edit().putString(KEY_LATEST_WEATHER, gson.toJson(weather)).apply()
    }

    private companion object {
        const val PREFS_NAME = "weather_preferences"
        const val KEY_SELECTED_LOCATION = "selected_location"
        const val KEY_LATEST_WEATHER = "latest_weather"
    }
}

internal data class SavedWeatherLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
)
