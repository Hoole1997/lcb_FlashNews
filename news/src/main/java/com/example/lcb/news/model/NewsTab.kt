package com.example.lcb.news.model

/**
 * 首页顶部 tab 配置。key 用于请求分页新闻，title 用于 UI 展示。
 */
data class NewsTab(
    val key: String,
    val title: String,
)
