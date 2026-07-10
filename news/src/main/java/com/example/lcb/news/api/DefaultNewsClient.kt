package com.example.lcb.news.api

import androidx.lifecycle.LifecycleOwner
import com.example.lcb.news.data.NewsRepository
import com.example.lcb.news.lifecycle.NewsLifecycleBinding
import com.example.lcb.news.lifecycle.NewsLifecycleHandle
import com.example.lcb.news.model.NewsArticle
import com.example.lcb.news.model.NewsArticleDetail
import com.example.lcb.news.model.NewsPage
import com.example.lcb.news.model.NewsTab

class DefaultNewsClient internal constructor(
    private val repository: NewsRepository,
) : NewsClient {

    override suspend fun getTabs(forceRefresh: Boolean): List<NewsTab> {
        return repository.tabs(forceRefresh)
    }

    override suspend fun getNewsPage(
        tabKey: String,
        offset: Int,
        limit: Int,
        refresh: Boolean,
    ): NewsPage {
        return repository.page(tabKey, offset, limit, refresh)
    }

    override suspend fun getNewsDetail(newsId: String): NewsArticleDetail? {
        return repository.detail(newsId)
    }

    override suspend fun getFavorites(): List<NewsArticle> {
        return repository.favorites()
    }

    override fun isFavorite(newsId: String): Boolean {
        return repository.isFavorite(newsId)
    }

    override fun rememberArticle(article: NewsArticle) {
        repository.rememberArticle(article)
    }

    override suspend fun toggleFavorite(article: NewsArticle): Boolean {
        return repository.toggleFavorite(article)
    }

    override fun bind(lifecycleOwner: LifecycleOwner): NewsLifecycleHandle {
        return NewsLifecycleBinding(lifecycleOwner, repository)
    }

    override fun clearMemory() {
        repository.clearMemory()
    }
}
