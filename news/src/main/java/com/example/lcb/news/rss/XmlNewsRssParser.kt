package com.example.lcb.news.rss

import android.util.Xml
import com.example.lcb.news.config.NewsFeedSourceConfig
import com.example.lcb.news.model.NewsArticle
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

class XmlNewsRssParser(
    private val fallbackParser: NewsRssParser = RegexNewsRssParser(),
) : NewsRssParser {

    override fun parse(xml: String, source: NewsFeedSourceConfig): List<NewsArticle> {
        return runCatching { parseXml(xml, source) }
            .getOrElse { fallbackParser.parse(xml, source) }
    }

    private fun parseXml(xml: String, source: NewsFeedSourceConfig): List<NewsArticle> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(StringReader(xml))
        }
        val result = ArrayList<NewsArticle>()
        var builder: FeedItemBuilder? = null
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name.orEmpty().lowercase()
                    if (name == "item" || name == "entry") {
                        builder = FeedItemBuilder()
                    } else {
                        builder?.let { readTag(parser, name, it) }
                    }
                }

                XmlPullParser.END_TAG -> {
                    val name = parser.name.orEmpty().lowercase()
                    if (name == "item" || name == "entry") {
                        builder?.build(source)?.let(result::add)
                        builder = null
                    }
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun readTag(parser: XmlPullParser, name: String, builder: FeedItemBuilder) {
        when (name) {
            "title" -> builder.title = parser.safeNextText()
            "link" -> {
                val href = parser.getAttributeValue(null, "href").orEmpty()
                builder.link = href.ifBlank { parser.safeNextText() }
            }
            "guid" -> {
                if (builder.link.isBlank()) {
                    builder.link = parser.safeNextText()
                }
            }
            "pubdate", "published", "updated", "dc:date" -> builder.date = parser.safeNextText()
            "description", "summary", "content:encoded", "encoded" -> {
                // 保留候选项，构建 Article 时选择清洗后真正包含文字的第一项。
                builder.addSummaryCandidate(parser.safeNextText())
            }
            "content" -> {
                val url = parser.getAttributeValue(null, "url").orEmpty()
                if (looksLikeImage(url)) {
                    if (builder.imageUrl.isBlank()) builder.imageUrl = url
                } else {
                    // Atom 的正文通常使用无前缀 content 标签。
                    builder.addSummaryCandidate(parser.safeNextText())
                }
            }
            "media:content", "media:thumbnail", "thumbnail", "enclosure" -> {
                val url = parser.getAttributeValue(null, "url").orEmpty()
                if (builder.imageUrl.isBlank() && looksLikeImage(url)) {
                    builder.imageUrl = url
                }
            }
        }
    }

    private fun XmlPullParser.safeNextText(): String {
        return runCatching { nextText() }.getOrDefault("")
    }

    private fun looksLikeImage(url: String): Boolean {
        if (url.isBlank()) return false
        return url.contains(".jpg", ignoreCase = true) ||
            url.contains(".jpeg", ignoreCase = true) ||
            url.contains(".png", ignoreCase = true) ||
            url.contains(".webp", ignoreCase = true) ||
            url.contains("image", ignoreCase = true)
    }
}
