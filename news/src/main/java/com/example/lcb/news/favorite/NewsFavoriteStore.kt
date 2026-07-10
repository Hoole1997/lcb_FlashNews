package com.example.lcb.news.favorite

import android.content.Context
import com.example.lcb.news.data.NewsArticleJson
import com.example.lcb.news.model.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class NewsFavoriteStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isFavorite(newsId: String): Boolean {
        return readRoot().has(newsId)
    }

    suspend fun toggle(article: NewsArticle): Boolean = withContext(Dispatchers.IO) {
        val root = readRoot()
        val favorite = if (root.has(article.id)) {
            root.remove(article.id)
            false
        } else {
            root.put(article.id, NewsArticleJson.toJson(article))
            true
        }
        prefs.edit().putString(KEY_ITEMS, root.toString()).apply()
        favorite
    }

    suspend fun find(newsId: String): NewsArticle? = withContext(Dispatchers.IO) {
        readRoot().optJSONObject(newsId)?.let(NewsArticleJson::fromJson)
    }

    suspend fun all(): List<NewsArticle> = withContext(Dispatchers.IO) {
        val root = readRoot()
        root.keys().asSequence()
            .mapNotNull { key -> root.optJSONObject(key)?.let(NewsArticleJson::fromJson) }
            .sortedByDescending { it.publishedAt }
            .toList()
    }

    private fun readRoot(): JSONObject {
        val raw = prefs.getString(KEY_ITEMS, null).orEmpty()
        return runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
    }

    private companion object {
        const val PREF_NAME = "news_sdk_favorites"
        const val KEY_ITEMS = "items_json"
    }
}
