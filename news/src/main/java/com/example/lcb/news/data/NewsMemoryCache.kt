package com.example.lcb.news.data

import com.example.lcb.news.model.NewsArticle

class NewsMemoryCache(private val maxArticles: Int) {
    private val articleMap = object : LinkedHashMap<String, NewsArticle>(maxArticles, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, NewsArticle>?): Boolean {
            return size > maxArticles
        }
    }

    @Synchronized
    fun putArticles(items: List<NewsArticle>) {
        items.forEach { articleMap[it.id] = it }
    }

    @Synchronized
    fun getArticle(id: String): NewsArticle? = articleMap[id]

    @Synchronized
    fun trimToHalf() {
        val target = maxArticles / 2
        while (articleMap.size > target) {
            articleMap.keys.firstOrNull()?.let { articleMap.remove(it) } ?: break
        }
    }

    @Synchronized
    fun clear() {
        articleMap.clear()
    }
}
