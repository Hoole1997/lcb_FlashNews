package com.example.lcb.news.model

data class NewsPage(
    val tabKey: String,
    val items: List<NewsArticle>,
    val nextOffset: Int,
    val hasMore: Boolean,
    val fromCache: Boolean,
)
