package com.example.lcb.news.detail

import com.example.lcb.news.model.NewsArticle
import com.example.lcb.news.model.NewsArticleDetail

class SummaryNewsArticleDetailProvider : NewsArticleDetailProvider {
    override suspend fun detail(article: NewsArticle): NewsArticleDetail {
        val paragraphs = article.summary
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(article.summary).filter { it.isNotBlank() } }
        return NewsArticleDetail(
            article = article,
            paragraphs = paragraphs,
            imageUrls = listOf(article.imageUrl).filter { it.isNotBlank() },
        )
    }
}
