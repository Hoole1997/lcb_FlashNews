package com.example.lcb.news.network

interface NewsHttpClient {
    suspend fun get(url: String, timeoutMillis: Int): String
}
