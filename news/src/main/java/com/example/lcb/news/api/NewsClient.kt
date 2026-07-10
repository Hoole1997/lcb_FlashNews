package com.example.lcb.news.api

import androidx.lifecycle.LifecycleOwner
import com.example.lcb.news.lifecycle.NewsLifecycleHandle
import com.example.lcb.news.model.NewsArticle
import com.example.lcb.news.model.NewsArticleDetail
import com.example.lcb.news.model.NewsPage
import com.example.lcb.news.model.NewsTab

interface NewsClient {
    suspend fun getTabs(forceRefresh: Boolean = false): List<NewsTab>

    /**
     * 返回一个 RSS 源对应的完整新闻批次。
     *
     * [limit] 为兼容既有 SDK 调用保留；RSS 源不支持服务端分页，因此当前实现不会截断单源结果。
     */
    suspend fun getNewsPage(
        tabKey: String,
        offset: Int = 0,
        limit: Int = DEFAULT_PAGE_SIZE,
        refresh: Boolean = false,
    ): NewsPage

    suspend fun getNewsDetail(newsId: String): NewsArticleDetail?
    suspend fun getFavorites(): List<NewsArticle>
    fun isFavorite(newsId: String): Boolean
    fun rememberArticle(article: NewsArticle)
    suspend fun toggleFavorite(article: NewsArticle): Boolean
    fun bind(lifecycleOwner: LifecycleOwner): NewsLifecycleHandle
    fun clearMemory()

    companion object {
        const val DEFAULT_PAGE_SIZE = 15
    }
}
