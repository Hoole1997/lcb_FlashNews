package com.example.lcb.app.news.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.browser.weather.data.WeatherData
import com.browser.weather.data.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeWeatherUiState(
    val data: WeatherData? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class HomeWeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WeatherRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(HomeWeatherUiState(data = repository.getCachedWeather()))
    val uiState: StateFlow<HomeWeatherUiState> = _uiState.asStateFlow()

    /**
     * 首页只展示天气摘要，数据缓存在 ViewModel 中，避免切换 Fragment 时重复请求。
     */
    fun loadWeatherIfNeeded(forceRefresh: Boolean = false) {
        val current = _uiState.value
        if (current.isLoading) return
        if (!forceRefresh && current.data != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.getPreferredWeather()
                .onSuccess { weather ->
                    _uiState.value = HomeWeatherUiState(data = weather)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message)
                    }
                }
        }
    }
}
