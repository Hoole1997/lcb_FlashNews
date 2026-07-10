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

class NewsCategoryViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val categoryKey: String
        get() = savedStateHandle[KEY_CATEGORY] ?: ""

    private val _uiState = MutableStateFlow(NewsCategoryUiState())
    val uiState: StateFlow<NewsCategoryUiState> = _uiState.asStateFlow()

    val savedScrollPosition: Int
        get() = savedStateHandle[KEY_SCROLL_POSITION] ?: 0
    val savedScrollOffset: Int
        get() = savedStateHandle[KEY_SCROLL_OFFSET] ?: 0

    fun loadInitial(client: NewsClient) {
        if (_uiState.value.loadedOnce || _uiState.value.loading) return
        loadPage(client = client, refresh = false)
    }

    fun bindCategory(categoryKey: String) {
        if (this.categoryKey.isBlank() && categoryKey.isNotBlank()) {
            savedStateHandle[KEY_CATEGORY] = categoryKey
        }
    }

    fun refresh(client: NewsClient) {
        loadPage(client = client, refresh = true)
    }

    fun loadMore(client: NewsClient) {
        val state = _uiState.value
        if (state.loading || !state.hasMore) return
        loadPage(client = client, refresh = false)
    }

    fun saveScroll(position: Int, offset: Int) {
        savedStateHandle[KEY_SCROLL_POSITION] = position.coerceAtLeast(0)
        savedStateHandle[KEY_SCROLL_OFFSET] = offset
    }

    private fun loadPage(client: NewsClient, refresh: Boolean) {
        val current = _uiState.value
        if (current.loading) return

        val requestOffset = if (refresh) 0 else current.nextOffset
        _uiState.value = current.copy(
            loading = true,
            refreshing = refresh,
            hasMore = if (refresh) true else current.hasMore,
        )

        viewModelScope.launch {
            val page = runCatching {
                client.getNewsPage(
                    tabKey = categoryKey,
                    offset = requestOffset,
                    refresh = refresh,
                )
            }.getOrNull()

            val latest = _uiState.value
            _uiState.value = if (page == null) {
                latest.copy(
                    loading = false,
                    refreshing = false,
                    // 首次请求失败也必须结束 Loading，避免永久占据页面。
                    loadedOnce = latest.loadedOnce || latest.items.isEmpty(),
                )
            } else {
                val mergedItems = if (refresh) page.items else latest.items + page.items
                latest.copy(
                    items = mergedItems.distinctBy(::articleIdentity),
                    nextOffset = page.nextOffset,
                    hasMore = page.hasMore,
                    loading = false,
                    refreshing = false,
                    loadedOnce = true,
                )
            }
        }
    }

    private companion object {
        private const val KEY_CATEGORY = "category_key"
        private const val KEY_SCROLL_POSITION = "news_category_scroll_position"
        private const val KEY_SCROLL_OFFSET = "news_category_scroll_offset"

        private fun articleIdentity(article: NewsArticle): String {
            return article.id.ifBlank { article.url }
        }
    }
}

data class NewsCategoryUiState(
    val items: List<NewsArticle> = emptyList(),
    val nextOffset: Int = 0,
    val hasMore: Boolean = true,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val loadedOnce: Boolean = false,
)
