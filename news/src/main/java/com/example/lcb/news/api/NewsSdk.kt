package com.example.lcb.news.api

import android.content.Context
import com.example.lcb.news.config.AssetNewsConfigDataSource
import com.example.lcb.news.config.DefaultNewsConfigRepository
import com.example.lcb.news.config.LocalNewsConfigStore
import com.example.lcb.news.config.NewsConfigParser
import com.example.lcb.news.config.NewsLanguage
import com.example.lcb.news.config.RemoteNewsConfigDataSource
import com.example.lcb.news.data.DefaultNewsAggregator
import com.example.lcb.news.data.DefaultNewsRepository
import com.example.lcb.news.data.NewsLocalArticleStore
import com.example.lcb.news.data.NewsMemoryCache
import com.example.lcb.news.detail.SummaryNewsArticleDetailProvider
import com.example.lcb.news.favorite.NewsFavoriteStore
import com.example.lcb.news.lifecycle.NewsTrimCallback
import com.example.lcb.news.model.NewsSdkOptions
import com.example.lcb.news.network.HttpUrlConnectionNewsHttpClient
import com.example.lcb.news.rss.XmlNewsRssParser

object NewsSdk {
    @Volatile
    private var holder: Holder? = null

    fun initialize(context: Context, options: NewsSdkOptions = NewsSdkOptions()): NewsClient {
        val languageKey = currentLanguageKey(context)
        holder?.takeIf { it.languageKey == languageKey }?.let { return it.client }
        return synchronized(this) {
            holder?.takeIf { it.languageKey == languageKey }?.client ?: run {
                releaseHolder(context.applicationContext)
                build(context.applicationContext, options, languageKey).also { holder = it }.client
            }
        }
    }

    fun client(context: Context): NewsClient {
        return initialize(context)
    }

    /**
     * 返回当前资源环境实际对应的新闻语言键。
     *
     * UI 可使用这个稳定键隔离不同语言的页面缓存，但不需要了解 SDK 内部的
     * locale 归一化、资源文件命名或远程配置命名规则。
     */
    fun languageKey(context: Context): String {
        return currentLanguageKey(context)
    }

    fun release(context: Context) {
        synchronized(this) {
            releaseHolder(context.applicationContext)
        }
    }

    private fun build(appContext: Context, options: NewsSdkOptions, languageKey: String): Holder {
        val configRepository = DefaultNewsConfigRepository(
            remoteDataSource = RemoteNewsConfigDataSource(options, languageKey),
            assetDataSource = AssetNewsConfigDataSource(appContext, options, languageKey),
            localStore = LocalNewsConfigStore(appContext),
            parser = NewsConfigParser(),
            options = options,
            languageKey = languageKey,
        )
        val memoryCache = NewsMemoryCache(options.maxMemoryArticles)
        val repository = DefaultNewsRepository(
            configRepository = configRepository,
            aggregator = DefaultNewsAggregator(
                httpClient = HttpUrlConnectionNewsHttpClient(),
                rssParser = XmlNewsRssParser(),
                options = options,
            ),
            memoryCache = memoryCache,
            localArticleStore = NewsLocalArticleStore(appContext),
            favoriteStore = NewsFavoriteStore(appContext),
            detailProvider = SummaryNewsArticleDetailProvider(),
            options = options,
        )
        val client = DefaultNewsClient(repository)
        val trimCallback = NewsTrimCallback(repository)
        appContext.registerComponentCallbacks(trimCallback)
        return Holder(client, trimCallback, languageKey)
    }

    private fun currentLanguageKey(context: Context): String {
        val localeTag = context.resources.configuration.locales.get(0)?.toLanguageTag()
        return NewsLanguage.fromLocaleTag(localeTag)
    }

    private fun releaseHolder(appContext: Context) {
        holder?.let {
            appContext.unregisterComponentCallbacks(it.trimCallback)
            it.client.clearMemory()
        }
        holder = null
    }

    private data class Holder(
        val client: NewsClient,
        val trimCallback: NewsTrimCallback,
        val languageKey: String,
    )
}
