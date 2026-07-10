package com.browser.weather.ui

import androidx.annotation.DrawableRes
import com.browser.weather.R

/**
 * WMO 天气代码到本地天气图标的统一映射。
 */
object WeatherIconMapper {
    @DrawableRes
    val defaultIconRes: Int = R.drawable.ic_weather_sunny

    @DrawableRes
    fun iconFor(weatherCode: Int, isDayTime: Boolean): Int {
        return when (weatherCode) {
            0 -> if (isDayTime) R.drawable.ic_weather_sunny else R.drawable.ic_weather_night_clear
            1 -> if (isDayTime) R.drawable.ic_weather_sunny else R.drawable.ic_weather_night_clear
            2 -> if (isDayTime) R.drawable.ic_weather_partly_cloudy else R.drawable.ic_weather_night_cloudy
            3 -> R.drawable.ic_weather_cloudy
            45, 48 -> R.drawable.ic_weather_fog
            51, 53, 55, 56, 57 -> R.drawable.ic_weather_rain
            61, 63, 65, 66, 67 -> R.drawable.ic_weather_rain
            71, 73, 75, 77 -> R.drawable.ic_weather_snow
            80, 81, 82 -> R.drawable.ic_weather_rain
            85, 86 -> R.drawable.ic_weather_snow
            95, 96, 99 -> R.drawable.ic_weather_thunderstorm
            else -> R.drawable.ic_weather_cloudy
        }
    }
}
