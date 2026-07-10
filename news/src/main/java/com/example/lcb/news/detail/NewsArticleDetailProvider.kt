package com.example.lcb.news.detail

import com.example.lcb.news.model.NewsArticle
import com.example.lcb.news.model.NewsArticleDetail

interface NewsArticleDetailProvider {
    suspend fun detail(article: NewsArticle): NewsArticleDetail
}
