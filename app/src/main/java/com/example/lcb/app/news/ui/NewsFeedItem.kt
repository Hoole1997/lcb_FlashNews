package com.example.lcb.app.news.ui

import com.example.lcb.news.model.NewsArticle

/**
 * 新闻列表的展示层模型。
 * 原始新闻数据保持纯净，广告只作为 RecyclerView 展示项插入。
 */
sealed class NewsFeedItem {
    data class News(val article: NewsArticle) : NewsFeedItem()
    data class NativeAd(val id: Int) : NewsFeedItem()
    data object LoadingMore : NewsFeedItem()
}
