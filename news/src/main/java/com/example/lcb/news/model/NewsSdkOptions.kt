package com.example.lcb.news.model

data class NewsSdkOptions(
    val remoteConfigKey: String = "flash_news_feed_config",
    val defaultAssetName: String = "news_default_feeds.json",
    val remoteConfigTimeoutMillis: Long = 2_500L,
    val requestTimeoutMillis: Int = 15_000,
    val pageSize: Int = 15,
    val maxConcurrentRequests: Int = 4,
    val maxMemoryArticles: Int = 240,
)
