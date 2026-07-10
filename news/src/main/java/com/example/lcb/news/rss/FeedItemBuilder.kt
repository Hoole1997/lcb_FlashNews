package com.example.lcb.news.rss

import com.example.lcb.news.config.NewsFeedSourceConfig
import com.example.lcb.news.model.NewsArticle
import com.example.lcb.news.util.NewsDate
import com.example.lcb.news.util.NewsIdFactory
import com.example.lcb.news.util.NewsText

internal class FeedItemBuilder {
    var title: String = ""
    var link: String = ""
    var imageUrl: String = ""
    private val summaryCandidates = ArrayList<String>(3)
    var date: String = ""

    fun addSummaryCandidate(value: String) {
        if (value.isNotBlank()) summaryCandidates += value
    }

    fun build(source: NewsFeedSourceConfig): NewsArticle? {
        val cleanTitle = NewsText.clean(title)
        val cleanLink = NewsText.decodeUrl(NewsText.clean(link))
        if (cleanTitle.isBlank() || cleanLink.isBlank()) return null

        val publishedAt = NewsDate.parse(date)
        return NewsArticle(
            id = NewsIdFactory.fromUrl(cleanLink),
            title = cleanTitle,
            url = cleanLink,
            imageUrl = NewsText.clean(imageUrl),
            summary = NewsText.firstUsableSummary(summaryCandidates),
            source = source.source,
            sourceLogoUrl = source.sourceLogoUrl,
            publishedAt = publishedAt,
            publishedText = NewsDate.display(publishedAt, date),
        )
    }
}
