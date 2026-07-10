package com.example.lcb.news.config

import org.json.JSONArray
import org.json.JSONObject

class NewsConfigParser {

    fun parse(json: String): NewsFeedConfig {
        val root = JSONObject(json)
        val ttlMinutes = root.optLong("cacheTtlMinutes", DEFAULT_TTL_MINUTES)
            .coerceAtLeast(1L)
        val categories = parseCategories(root.getJSONArray("categories"))
        require(categories.isNotEmpty()) { "news categories is empty" }
        return NewsFeedConfig(
            version = root.optInt("version", 1),
            cacheTtlMillis = ttlMinutes * 60_000L,
            categories = categories,
        )
    }

    private fun parseCategories(array: JSONArray): List<NewsCategoryConfig> {
        val result = ArrayList<NewsCategoryConfig>(array.length())
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val key = item.optString("key").trim()
            val title = item.optString("title", item.optString("label")).trim()
            val feeds = parseFeeds(item.optJSONArray("feeds"))
            if (key.isNotBlank() && title.isNotBlank() && feeds.isNotEmpty()) {
                result += NewsCategoryConfig(key = key, title = title, feeds = feeds)
            }
        }
        return result
    }

    private fun parseFeeds(array: JSONArray?): List<NewsFeedSourceConfig> {
        if (array == null) return emptyList()
        val result = ArrayList<NewsFeedSourceConfig>(array.length())
        for (index in 0 until array.length()) {
            val value = array.get(index)
            val source = when (value) {
                is JSONObject -> value.optString("source")
                is JSONArray -> value.optString(0)
                else -> ""
            }.trim()
            val url = when (value) {
                is JSONObject -> value.optString("url")
                is JSONArray -> value.optString(1)
                else -> ""
            }.trim()
            val weight = when (value) {
                is JSONObject -> value.optInt("weight", 1)
                else -> 1
            }.coerceAtLeast(1)
            val sourceLogoUrl = when (value) {
                is JSONObject -> value.optString("sourceLogoUrl")
                else -> ""
            }.trim()
            if (source.isNotBlank() && url.startsWith("http")) {
                result += NewsFeedSourceConfig(
                    source = source,
                    url = url,
                    weight = weight,
                    sourceLogoUrl = sourceLogoUrl,
                )
            }
        }
        return result
    }

    private companion object {
        const val DEFAULT_TTL_MINUTES = 10L
    }
}
