package com.example.lcb.app.news.ui

import com.example.lcb.news.model.NewsArticle

interface FlashNewsNavigator {
    fun openNewsDetail(article: NewsArticle)
}
