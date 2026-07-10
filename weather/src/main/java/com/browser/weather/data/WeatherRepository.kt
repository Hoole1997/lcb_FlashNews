package com.browser.weather.data

import android.content.Context

/**
 * 天气数据仓库
 * 封装 API 调用，提供简洁接口给 ViewModel
 */
class WeatherRepository(context: Context) {

    private val apiClient = WeatherApiClient(context)
    private val preferenceStore = WeatherPreferenceStore(context)

    /** 优先使用用户手选城市；未选择时才使用 IP 定位。 */
    suspend fun getPreferredWeather(): Result<WeatherData> {
        val selected = preferenceStore.selectedLocation()
        val result = if (selected == null) {
            apiClient.getWeatherByIp()
        } else {
            apiClient.getWeatherByLatLon(selected.latitude, selected.longitude, selected.cityName)
        }
        return cacheSuccessOrFallback(result)
    }

    fun getCachedWeather(): WeatherData? = preferenceStore.latestWeather()

    /**
     * 通过 IP 获取当前位置天气
     */
    suspend fun getWeatherByIp(): Result<WeatherData> {
        return cacheSuccessOrFallback(apiClient.getWeatherByIp())
    }

    /**
     * 通过经纬度和城市名获取天气
     */
    suspend fun getWeatherByLatLon(lat: Double, lon: Double, cityName: String): Result<WeatherData> {
        val result = apiClient.getWeatherByLatLon(lat, lon, cityName)
        result.onSuccess { weather ->
            preferenceStore.saveSelectedLocation(SavedWeatherLocation(lat, lon, cityName))
            preferenceStore.saveLatestWeather(weather)
        }
        return result
    }

    /**
     * 搜索城市
     */
    suspend fun searchCity(query: String): Result<List<GeocodingResult>> {
        return apiClient.searchCity(query)
    }

    private fun cacheSuccessOrFallback(result: Result<WeatherData>): Result<WeatherData> {
        result.onSuccess(preferenceStore::saveLatestWeather)
        if (result.isSuccess) return result
        return preferenceStore.latestWeather()?.let(Result.Companion::success) ?: result
    }
}
