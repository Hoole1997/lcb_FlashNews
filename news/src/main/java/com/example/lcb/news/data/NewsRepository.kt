package com.example.lcb.news.data

import com.example.lcb.news.model.NewsArticle
import com.example.lcb.news.model.NewsArticleDetail
import com.example.lcb.news.model.NewsPage
import com.example.lcb.news.model.NewsTab

interface NewsRepository {
    suspend fun tabs(forceRefresh: Boolean = false): List<NewsTab>
    suspend fun page(tabKey: String, offset: Int, limit: Int, refresh: Boolean = false): NewsPage
    suspend fun detail(newsId: String): NewsArticleDetail?
    suspend fun favorites(): List<NewsArticle>
    fun isFavorite(newsId: String): Boolean
    fun rememberArticle(article: NewsArticle)
    suspend fun toggleFavorite(article: NewsArticle): Boolean
    fun clearMemory()
    fun trimMemory()
}
