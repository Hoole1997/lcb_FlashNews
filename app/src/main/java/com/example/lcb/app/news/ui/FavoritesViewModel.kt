package com.example.lcb.app.news.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lcb.news.api.NewsClient
import com.example.lcb.news.model.NewsArticle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    val savedScrollPosition: Int
        get() = savedStateHandle[KEY_SCROLL_POSITION] ?: 0
    val savedScrollOffset: Int
        get() = savedStateHandle[KEY_SCROLL_OFFSET] ?: 0

    fun load(client: NewsClient) {
        if (_uiState.value.loading) return
        _uiState.value = _uiState.value.copy(loading = true)
        viewModelScope.launch {
            val favorites = runCatching { client.getFavorites() }.getOrDefault(_uiState.value.items)
            _uiState.value = FavoritesUiState(items = favorites, loading = false, loadedOnce = true)
        }
    }

    fun toggleFavorite(client: NewsClient, article: NewsArticle) {
        if (_uiState.value.loading) return
        _uiState.value = _uiState.value.copy(loading = true)
        viewModelScope.launch {
            runCatching { client.toggleFavorite(article) }
            val favorites = runCatching { client.getFavorites() }.getOrDefault(_uiState.value.items)
            _uiState.value = FavoritesUiState(items = favorites, loading = false, loadedOnce = true)
        }
    }

    fun saveScroll(position: Int, offset: Int) {
        savedStateHandle[KEY_SCROLL_POSITION] = position.coerceAtLeast(0)
        savedStateHandle[KEY_SCROLL_OFFSET] = offset
    }

    private companion object {
        private const val KEY_SCROLL_POSITION = "favorites_scroll_position"
        private const val KEY_SCROLL_OFFSET = "favorites_scroll_offset"
    }
}

data class FavoritesUiState(
    val items: List<NewsArticle> = emptyList(),
    val loading: Boolean = false,
    val loadedOnce: Boolean = false,
)
