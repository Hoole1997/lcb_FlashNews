package com.example.lcb.news.config

import com.example.lcb.news.model.NewsTab

data class NewsFeedConfig(
    val version: Int,
    val cacheTtlMillis: Long,
    val categories: List<NewsCategoryConfig>,
) {
    val tabs: List<NewsTab> = categories.map { NewsTab(it.key, it.title) }

    fun category(key: String): NewsCategoryConfig? = categories.firstOrNull { it.key == key }
}

data class NewsCategoryConfig(
    val key: String,
    val title: String,
    val feeds: List<NewsFeedSourceConfig>,
)

data class NewsFeedSourceConfig(
    val source: String,
    val url: String,
    val weight: Int,
    val sourceLogoUrl: String = "",
)
