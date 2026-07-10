package com.example.lcb.news.rss

import com.example.lcb.news.config.NewsFeedSourceConfig
import com.example.lcb.news.model.NewsArticle
import com.example.lcb.news.util.NewsDate
import com.example.lcb.news.util.NewsIdFactory
import com.example.lcb.news.util.NewsText

class RegexNewsRssParser : NewsRssParser {
    private val itemRegex = Regex("<item[ >].*?</item>", regexOptions)
    private val entryRegex = Regex("<entry[ >].*?</entry>", regexOptions)

    override fun parse(xml: String, source: NewsFeedSourceConfig): List<NewsArticle> {
        val blocks = itemRegex.findAll(xml).map { it.value }.toList().ifEmpty {
            entryRegex.findAll(xml).map { it.value }.toList()
        }
        return blocks.mapNotNull { block -> parseBlock(block, source) }
    }

    private fun parseBlock(block: String, source: NewsFeedSourceConfig): NewsArticle? {
        val title = NewsText.clean(first("<title[^>]*>(.*?)</title>", block))
        var link = first("<link[^>]*>(https?://[^<]+)</link>", block)
        if (link.isBlank()) {
            link = first("<link[^>]+href=[\"']([^\"']+)[\"']", block)
        }
        if (title.isBlank() || link.isBlank()) return null

        val date = first("<pubDate>(.*?)</pubDate>", block)
            .ifBlank { first("<published>(.*?)</published>", block) }
            .ifBlank { first("<updated>(.*?)</updated>", block) }
            .ifBlank { first("<dc:date>(.*?)</dc:date>", block) }
        val publishedAt = NewsDate.parse(date)
        val summary = NewsText.firstUsableSummary(
            listOf(
                first("<description[^>]*>(.*?)</description>", block),
                first("<summary[^>]*>(.*?)</summary>", block),
                first("<content:encoded[^>]*>(.*?)</content:encoded>", block),
            ),
        )

        return NewsArticle(
            id = NewsIdFactory.fromUrl(link),
            title = title,
            url = NewsText.decodeUrl(link.trim()),
            imageUrl = image(block),
            summary = summary,
            source = source.source,
            sourceLogoUrl = source.sourceLogoUrl,
            publishedAt = publishedAt,
            publishedText = NewsDate.display(publishedAt, date),
        )
    }

    private fun first(pattern: String, block: String): String {
        return Regex(pattern, regexOptions).find(block)?.groupValues?.getOrNull(1)?.trim().orEmpty()
    }

    private fun image(block: String): String {
        val patterns = listOf(
            "<media:(?:content|thumbnail)[^>]+url=[\"']([^\"']+)[\"']",
            "<enclosure[^>]+url=[\"']([^\"']+\\.(?:jpg|jpeg|png|webp)[^\"']*)[\"']",
            "<img[^>]+src=[\"']([^\"']+)[\"']",
        )
        return patterns.firstNotNullOfOrNull { pattern ->
            Regex(pattern, RegexOption.IGNORE_CASE).find(block)?.groupValues?.getOrNull(1)
        }.orEmpty()
    }

    private companion object {
        val regexOptions = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    }
}
