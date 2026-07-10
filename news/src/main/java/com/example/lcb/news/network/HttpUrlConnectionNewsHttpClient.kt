package com.example.lcb.news.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

class HttpUrlConnectionNewsHttpClient : NewsHttpClient {

    override suspend fun get(url: String, timeoutMillis: Int): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = timeoutMillis
        connection.readTimeout = timeoutMillis
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
        connection.setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
        // 部分多语言媒体（例如 Live Hindustan）只在显式声明 gzip 时返回完整 RSS 正文。
        connection.setRequestProperty("Accept-Encoding", "gzip")

        try {
            val code = connection.responseCode
            val rawStream = if (code in 200..299) connection.inputStream else connection.errorStream
            val stream = if ("gzip".equals(connection.contentEncoding, ignoreCase = true)) {
                GZIPInputStream(rawStream)
            } else {
                rawStream
            }
            stream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
    }
}
