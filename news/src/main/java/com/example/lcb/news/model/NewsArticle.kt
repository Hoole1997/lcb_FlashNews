package com.example.lcb.news.model

/**
 * 新闻列表和收藏共用的稳定模型。
 */
data class NewsArticle(
    val id: String,
    val title: String,
    val url: String,
    val imageUrl: String,
    val summary: String,
    val source: String,
    val sourceLogoUrl: String,
    val publishedAt: Long,
    val publishedText: String,
    val sourceCount: Int = 1,
    val clusterImageUrls: List<String> = emptyList(),
)
