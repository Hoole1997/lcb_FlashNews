package com.example.lcb.news.model

data class NewsArticleDetail(
    val article: NewsArticle,
    val author: String = "",
    val paragraphs: List<String> = emptyList(),
    val imageUrls: List<String> = emptyList(),
)
