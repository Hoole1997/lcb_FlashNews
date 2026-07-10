package com.example.lcb.news.data

import android.content.Context
import com.example.lcb.news.model.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class NewsLocalArticleStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    suspend fun saveRecent(items: List<NewsArticle>, maxCount: Int) = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext
        val root = readRoot()
        items.forEach { root.put(it.id, NewsArticleJson.toJson(it)) }
        while (root.length() > maxCount) {
            root.keys().asSequence().firstOrNull()?.let { root.remove(it) } ?: break
        }
        prefs.edit().putString(KEY_RECENT, root.toString()).apply()
    }

    suspend fun find(id: String): NewsArticle? = withContext(Dispatchers.IO) {
        readRoot().optJSONObject(id)?.let(NewsArticleJson::fromJson)
    }

    private fun readRoot(): JSONObject {
        val raw = prefs.getString(KEY_RECENT, null).orEmpty()
        return runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
    }

    private companion object {
        const val PREF_NAME = "news_sdk_recent_articles"
        const val KEY_RECENT = "recent_json"
    }
}
