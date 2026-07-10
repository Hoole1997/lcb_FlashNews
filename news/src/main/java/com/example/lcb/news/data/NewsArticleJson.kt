package com.example.lcb.news.data

import com.example.lcb.news.model.NewsArticle
import org.json.JSONArray
import org.json.JSONObject

internal object NewsArticleJson {

    fun toJson(article: NewsArticle): JSONObject {
        return JSONObject()
            .put("id", article.id)
            .put("title", article.title)
            .put("url", article.url)
            .put("imageUrl", article.imageUrl)
            .put("summary", article.summary)
            .put("source", article.source)
            .put("sourceLogoUrl", article.sourceLogoUrl)
            .put("publishedAt", article.publishedAt)
            .put("publishedText", article.publishedText)
            .put("sourceCount", article.sourceCount)
            .put("clusterImageUrls", JSONArray(article.clusterImageUrls))
    }

    fun fromJson(json: JSONObject): NewsArticle {
        return NewsArticle(
            id = json.optString("id"),
            title = json.optString("title"),
            url = json.optString("url"),
            imageUrl = json.optString("imageUrl"),
            summary = json.optString("summary"),
            source = json.optString("source"),
            sourceLogoUrl = json.optString("sourceLogoUrl"),
            publishedAt = json.optLong("publishedAt", 0L),
            publishedText = json.optString("publishedText"),
            sourceCount = json.optInt("sourceCount", 1),
            clusterImageUrls = json.optJSONArray("clusterImageUrls").toStringList(),
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return List(length()) { index -> optString(index) }.filter { it.isNotBlank() }
    }
}
