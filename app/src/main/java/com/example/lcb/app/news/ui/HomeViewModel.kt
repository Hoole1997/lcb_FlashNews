package com.example.lcb.app.news.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.lcb.news.model.NewsTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _tabs = MutableStateFlow<List<NewsTab>>(emptyList())
    val tabs: StateFlow<List<NewsTab>> = _tabs.asStateFlow()

    var selectedPage: Int
        get() = savedStateHandle[KEY_SELECTED_PAGE] ?: 0
        set(value) {
            savedStateHandle[KEY_SELECTED_PAGE] = value.coerceAtLeast(0)
        }

    /**
     * 将首页缓存绑定到实际新闻语言。配置重建会保留 ViewModel，因此语言变化时
     * 必须主动清除旧 Tab；普通旋转等同语言重建仍继续复用原状态。
     */
    fun bindLanguage(languageKey: String): Boolean {
        val previousLanguageKey = savedStateHandle.get<String>(KEY_LANGUAGE)
        savedStateHandle[KEY_LANGUAGE] = languageKey
        if (previousLanguageKey == null || previousLanguageKey == languageKey) {
            return false
        }

        _tabs.value = emptyList()
        selectedPage = 0
        return true
    }

    fun setTabs(tabs: List<NewsTab>) {
        _tabs.value = tabs
        if (tabs.isNotEmpty()) {
            selectedPage = selectedPage.coerceIn(0, tabs.lastIndex)
        }
    }

    private companion object {
        private const val KEY_LANGUAGE = "home_news_language"
        private const val KEY_SELECTED_PAGE = "home_selected_page"
    }
}
