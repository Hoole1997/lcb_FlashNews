package com.example.lcb.news.rss

import com.example.lcb.news.config.NewsFeedSourceConfig
import com.example.lcb.news.model.NewsArticle

interface NewsRssParser {
    fun parse(xml: String, source: NewsFeedSourceConfig): List<NewsArticle>
}
