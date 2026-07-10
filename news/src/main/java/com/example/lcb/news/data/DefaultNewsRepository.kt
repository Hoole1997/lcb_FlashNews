package com.example.lcb.news.data

import com.example.lcb.news.config.DefaultNewsConfigRepository
import com.example.lcb.news.detail.NewsArticleDetailProvider
import com.example.lcb.news.favorite.NewsFavoriteStore
import com.example.lcb.news.model.NewsArticle
import com.example.lcb.news.model.NewsArticleDetail
import com.example.lcb.news.model.NewsPage
import com.example.lcb.news.model.NewsSdkOptions
import com.example.lcb.news.model.NewsTab

class DefaultNewsRepository(
    private val configRepository: DefaultNewsConfigRepository,
    private val aggregator: DefaultNewsAggregator,
    private val memoryCache: NewsMemoryCache,
    private val localArticleStore: NewsLocalArticleStore,
    private val favoriteStore: NewsFavoriteStore,
    private val detailProvider: NewsArticleDetailProvider,
    private val options: NewsSdkOptions,
) : NewsRepository {

    override suspend fun tabs(forceRefresh: Boolean): List<NewsTab> {
        return configRepository.getConfig(forceRemote = forceRefresh).tabs
    }

    @Suppress("UNUSED_PARAMETER")
    override suspend fun page(tabKey: String, offset: Int, limit: Int, refresh: Boolean): NewsPage {
        val config = configRepository.getConfig(forceRemote = false)
        val category = config.category(tabKey) ?: config.categories.first()
        val loadPolicy = when {
            refresh -> NewsCategoryLoadPolicy.FORCE_REFRESH
            offset > 0 -> NewsCategoryLoadPolicy.NEXT_FEED
            else -> NewsCategoryLoadPolicy.CACHE_WITH_TTL
        }

        val result = aggregator.page(
            category = category,
            offset = offset,
            cacheTtlMillis = config.cacheTtlMillis,
            loadPolicy = loadPolicy,
        )
        memoryCache.putArticles(result.items)
        localArticleStore.saveRecent(result.items, options.maxMemoryArticles)

        return NewsPage(
            tabKey = category.key,
            items = result.items,
            nextOffset = result.nextOffset,
            hasMore = result.hasMore,
            fromCache = result.fromCache,
        )
    }

    override suspend fun detail(newsId: String): NewsArticleDetail? {
        val article = memoryCache.getArticle(newsId)
            ?: favoriteStore.find(newsId)
            ?: localArticleStore.find(newsId)
            ?: return null
        return detailProvider.detail(article)
    }

    override suspend fun favorites(): List<NewsArticle> = favoriteStore.all()

    override fun isFavorite(newsId: String): Boolean = favoriteStore.isFavorite(newsId)

    override fun rememberArticle(article: NewsArticle) {
        // 点击列表进入详情前先把完整对象放入内存，避免详情页只凭 id 反查时遇到生命周期缓存空窗。
        memoryCache.putArticles(listOf(article))
    }

    override suspend fun toggleFavorite(article: NewsArticle): Boolean {
        memoryCache.putArticles(listOf(article))
        localArticleStore.saveRecent(listOf(article), options.maxMemoryArticles)
        return favoriteStore.toggle(article)
    }

    override fun clearMemory() {
        aggregator.clear()
        configRepository.clearMemory()
        memoryCache.clear()
    }

    override fun trimMemory() {
        memoryCache.trimToHalf()
    }
}
