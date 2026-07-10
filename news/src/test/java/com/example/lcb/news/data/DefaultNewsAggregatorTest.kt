package com.example.lcb.news.data

import com.example.lcb.news.config.NewsCategoryConfig
import com.example.lcb.news.config.NewsFeedSourceConfig
import com.example.lcb.news.model.NewsArticle
import com.example.lcb.news.model.NewsSdkOptions
import com.example.lcb.news.network.NewsHttpClient
import com.example.lcb.news.rss.NewsRssParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultNewsAggregatorTest {
    private val feeds = (1..3).map { index ->
        NewsFeedSourceConfig(
            source = "Publisher$index",
            url = "https://example.com/rss/$index",
            weight = 1,
        )
    }
    private val category = NewsCategoryConfig(
        key = "test",
        title = "Test",
        feeds = feeds,
    )

    @Test
    fun firstPageRequestsOnlyFirstFeedAndReturnsEveryArticle() = runBlocking {
        val httpClient = CountingHttpClient()
        val aggregator = createAggregator(httpClient)

        val page = aggregator.page(
            category = category,
            offset = 0,
            cacheTtlMillis = 60_000,
            loadPolicy = NewsCategoryLoadPolicy.CACHE_WITH_TTL,
        )

        assertEquals(listOf(feeds[0].url), httpClient.requestedUrls)
        assertEquals(20, page.items.size)
        assertEquals(20, page.nextOffset)
        assertTrue(page.hasMore)
        assertFalse(page.fromCache)
    }

    @Test
    fun loadMoreRequestsNextFeedAndReturnsItsWholeBatch() = runBlocking {
        val httpClient = CountingHttpClient()
        val aggregator = createAggregator(httpClient)
        val firstPage = aggregator.page(
            category = category,
            offset = 0,
            cacheTtlMillis = 60_000,
            loadPolicy = NewsCategoryLoadPolicy.CACHE_WITH_TTL,
        )

        val secondPage = aggregator.page(
            category = category,
            offset = firstPage.nextOffset,
            cacheTtlMillis = 60_000,
            loadPolicy = NewsCategoryLoadPolicy.NEXT_FEED,
        )

        assertEquals(listOf(feeds[0].url, feeds[1].url), httpClient.requestedUrls)
        assertEquals(20, secondPage.items.size)
        assertEquals(40, secondPage.nextOffset)
        assertTrue(secondPage.hasMore)
    }

    @Test
    fun cachedFirstPageDoesNotRefetch() = runBlocking {
        val httpClient = CountingHttpClient()
        val aggregator = createAggregator(httpClient)
        aggregator.page(category, 0, 60_000, NewsCategoryLoadPolicy.CACHE_WITH_TTL)

        val cachedPage = aggregator.page(
            category = category,
            offset = 0,
            cacheTtlMillis = 60_000,
            loadPolicy = NewsCategoryLoadPolicy.CACHE_WITH_TTL,
        )

        assertEquals(1, httpClient.requestedUrls.size)
        assertEquals(20, cachedPage.items.size)
        assertTrue(cachedPage.fromCache)
    }

    @Test
    fun forceRefreshRestartsFromFirstFeed() = runBlocking {
        val httpClient = CountingHttpClient()
        val aggregator = createAggregator(httpClient)
        aggregator.page(category, 0, 60_000, NewsCategoryLoadPolicy.CACHE_WITH_TTL)

        aggregator.page(category, 0, 60_000, NewsCategoryLoadPolicy.FORCE_REFRESH)

        assertEquals(listOf(feeds[0].url, feeds[0].url), httpClient.requestedUrls)
    }

    private fun createAggregator(httpClient: CountingHttpClient): DefaultNewsAggregator {
        return DefaultNewsAggregator(
            httpClient = httpClient,
            rssParser = StaticNewsParser(),
            options = NewsSdkOptions(),
        )
    }

    private class CountingHttpClient : NewsHttpClient {
        val requestedUrls = ArrayList<String>()

        override suspend fun get(url: String, timeoutMillis: Int): String {
            requestedUrls += url
            return url
        }
    }

    private class StaticNewsParser : NewsRssParser {
        override fun parse(xml: String, source: NewsFeedSourceConfig): List<NewsArticle> {
            return (1..20).map { index ->
                NewsArticle(
                    id = "${source.source}-$index",
                    title = "${source.source} article topic$index",
                    url = "${source.url}/article/$index",
                    imageUrl = "",
                    summary = "",
                    source = source.source,
                    sourceLogoUrl = "",
                    publishedAt = System.currentTimeMillis() - index,
                    publishedText = "today",
                )
            }
        }
    }
}
