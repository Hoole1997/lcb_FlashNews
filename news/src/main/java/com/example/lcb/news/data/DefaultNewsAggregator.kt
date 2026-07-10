package com.example.lcb.news.data

import com.example.lcb.news.config.NewsCategoryConfig
import com.example.lcb.news.model.NewsArticle
import com.example.lcb.news.model.NewsSdkOptions
import com.example.lcb.news.network.NewsHttpClient
import com.example.lcb.news.rss.NewsRssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultNewsAggregator(
    private val httpClient: NewsHttpClient,
    private val rssParser: NewsRssParser,
    private val options: NewsSdkOptions,
) {
    private data class CategorySession(
        var timestamp: Long,
        val items: MutableList<NewsArticle> = ArrayList(),
        val batches: MutableList<List<NewsArticle>> = ArrayList(),
        var nextFeedIndex: Int = 0,
    ) {
        val itemCount: Int
            get() = items.size

        fun pageAt(offset: Int): Pair<List<NewsArticle>, Int>? {
            var batchStart = 0
            for (batch in batches) {
                val batchEnd = batchStart + batch.size
                if (offset in batchStart until batchEnd) {
                    return batch.drop(offset - batchStart) to batchEnd
                }
                batchStart = batchEnd
            }
            return null
        }
    }

    internal data class PageResult(
        val items: List<NewsArticle>,
        val nextOffset: Int,
        val hasMore: Boolean,
        val fromCache: Boolean,
    )

    private val sessions = HashMap<String, CategorySession>()

    internal suspend fun page(
        category: NewsCategoryConfig,
        offset: Int,
        cacheTtlMillis: Long,
        loadPolicy: NewsCategoryLoadPolicy,
    ): PageResult = withContext(Dispatchers.IO) {
        val safeOffset = offset.coerceAtLeast(0)
        val session = session(category, cacheTtlMillis, loadPolicy)
        var requestedFeed = false
        var page = session.pageAt(safeOffset)

        // 一个 RSS 就是一页；失败或全量重复时继续尝试后续源，避免空页卡死。
        while (page == null && safeOffset >= session.itemCount && session.nextFeedIndex < category.feeds.size) {
            val feed = category.feeds[session.nextFeedIndex++]
            val parsedItems = runCatching {
                rssParser.parse(httpClient.get(feed.url, options.requestTimeoutMillis), feed)
            }.getOrDefault(emptyList())
            requestedFeed = true
            session.timestamp = System.currentTimeMillis()

            val newItems = parsedItems
                .distinctBy { it.articleIdentity }
                .sortedByDescending(NewsArticle::publishedAt)
                .filterNot { candidate -> isDuplicate(candidate, session.items) }
            if (newItems.isNotEmpty()) {
                session.batches += newItems
                session.items += newItems
                page = newItems to session.itemCount
            }
        }

        val pageItems = page?.first.orEmpty()
        val nextOffset = page?.second ?: safeOffset
        PageResult(
            items = pageItems,
            nextOffset = nextOffset,
            hasMore = nextOffset < session.itemCount || session.nextFeedIndex < category.feeds.size,
            fromCache = !requestedFeed,
        )
    }

    fun clear() {
        sessions.clear()
    }

    private fun session(
        category: NewsCategoryConfig,
        cacheTtlMillis: Long,
        loadPolicy: NewsCategoryLoadPolicy,
    ): CategorySession {
        val cached = sessions[category.key]
        val shouldReset = when (loadPolicy) {
            NewsCategoryLoadPolicy.FORCE_REFRESH -> true
            NewsCategoryLoadPolicy.CACHE_WITH_TTL -> cached == null ||
                System.currentTimeMillis() - cached.timestamp >= cacheTtlMillis
            NewsCategoryLoadPolicy.NEXT_FEED -> cached == null
        }
        return if (shouldReset) {
            CategorySession(timestamp = System.currentTimeMillis()).also {
                sessions[category.key] = it
            }
        } else {
            requireNotNull(cached)
        }
    }

    private fun normalizedWords(title: String): Set<String> {
        return Regex("[^a-z0-9 ]")
            .replace(title.lowercase(), " ")
            .split(" ")
            .filter { it.length > 2 && it !in STOP_WORDS }
            .toSet()
    }

    private fun jaccard(left: Set<String>, right: Set<String>): Double {
        if (left.isEmpty() || right.isEmpty()) return 0.0
        return left.intersect(right).size.toDouble() / left.union(right).size
    }

    private fun isDuplicate(candidate: NewsArticle, existingItems: List<NewsArticle>): Boolean {
        if (existingItems.any { it.articleIdentity == candidate.articleIdentity }) return true
        val candidateWords = normalizedWords(candidate.title)
        return existingItems.any { existing ->
            jaccard(candidateWords, normalizedWords(existing.title)) >= DEDUP_THRESHOLD
        }
    }

    private val NewsArticle.articleIdentity: String
        get() = id.ifBlank { url }

    private companion object {
        const val DEDUP_THRESHOLD = 0.6
        val STOP_WORDS = (
            "the a an of to in on for and or as at by with from is are was be new " +
                "says after over into amid ahead its his her their this that"
            ).split(" ").toSet()
    }
}
