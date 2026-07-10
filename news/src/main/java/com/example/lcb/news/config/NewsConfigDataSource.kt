package com.example.lcb.news.config

interface NewsConfigDataSource {
    suspend fun read(forceRefresh: Boolean = false): String?
}
