package com.example.lcb.app.news.ui

import com.example.lcb.news.model.NewsArticle

/**
 * 新闻流广告插入规则集中管理，避免首页/收藏页各自写一套位置计算。
 */
object NewsAdInsertionPolicy {
    private const val INLINE_NATIVE_AD_INTERVAL = 4

    fun uniqueArticles(articles: List<NewsArticle>): List<NewsArticle> {
        return articles.distinctBy(::articleIdentity)
    }

    fun buildFeedItems(
        articles: List<NewsArticle>,
        appendTailAdWhenShort: Boolean = false,
    ): List<NewsFeedItem> {
        val uniqueArticles = uniqueArticles(articles)
        if (uniqueArticles.isEmpty()) return emptyList()

        val result = mutableListOf<NewsFeedItem>()
        var adId = 0
        var newsCountSinceLastAd = 0

        uniqueArticles.forEach { article ->
            result.add(NewsFeedItem.News(article))
            newsCountSinceLastAd++

            if (newsCountSinceLastAd == INLINE_NATIVE_AD_INTERVAL) {
                result.add(NewsFeedItem.NativeAd(adId++))
                newsCountSinceLastAd = 0
            }
        }

        // 收藏页 1-3 条新闻时也要在新闻下方补一个广告位；空列表不展示广告。
        if (appendTailAdWhenShort && uniqueArticles.size in 1 until INLINE_NATIVE_AD_INTERVAL) {
            result.add(NewsFeedItem.NativeAd(adId))
        }

        return result
    }

    private fun articleIdentity(article: NewsArticle): String {
        return article.id.ifBlank { article.url }
    }
}
