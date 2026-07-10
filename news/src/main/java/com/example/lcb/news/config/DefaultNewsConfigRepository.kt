package com.example.lcb.news.config

import com.example.lcb.news.model.NewsSdkOptions
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

class DefaultNewsConfigRepository(
    private val remoteDataSource: NewsConfigDataSource,
    private val assetDataSource: NewsConfigDataSource,
    private val localStore: LocalNewsConfigStore,
    private val parser: NewsConfigParser,
    private val options: NewsSdkOptions,
    private val languageKey: String,
) {
    private val mutex = Mutex()

    @Volatile
    private var memoryConfig: NewsFeedConfig? = null

    suspend fun getConfig(forceRemote: Boolean = false): NewsFeedConfig = mutex.withLock {
        if (!forceRemote) {
            memoryConfig?.let { return@withLock it }
        }

        val candidates = listOfNotNull(
            parseRemote(forceRemote),
            parseLocalLastGood(),
            parseAsset(),
        )
        // 安装包可能携带比远程缓存更新的紧急修正版；版本号优先，版本相同时仍保持远程优先。
        val newest = candidates.maxByOrNull(NewsFeedConfig::version)
            ?: error("No valid news feed config found")
        cache(newest)
    }

    fun clearMemory() {
        memoryConfig = null
    }

    private suspend fun parseRemote(forceRemote: Boolean): NewsFeedConfig? {
        val json = withTimeoutOrNull(options.remoteConfigTimeoutMillis) {
            remoteDataSource.read(forceRemote)
        } ?: return null
        return parseOrNull(json)?.also { localStore.saveLastGood(languageKey, json) }
    }

    private suspend fun parseLocalLastGood(): NewsFeedConfig? {
        return localStore.readLastGood(languageKey)?.let { parseOrNull(it) }
    }

    private suspend fun parseAsset(): NewsFeedConfig? {
        return assetDataSource.read()?.let { parseOrNull(it) }
    }

    private fun parseOrNull(json: String): NewsFeedConfig? {
        return runCatching { parser.parse(json) }.getOrNull()
    }

    private fun cache(config: NewsFeedConfig): NewsFeedConfig {
        memoryConfig = config
        return config
    }
}
