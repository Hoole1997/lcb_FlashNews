package com.example.lcb.news.config

import com.example.lcb.news.model.NewsSdkOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.corekit.core.utils.ConfigRemoteManager

class RemoteNewsConfigDataSource(
    private val options: NewsSdkOptions,
    private val languageKey: String,
) : NewsConfigDataSource {

    override suspend fun read(forceRefresh: Boolean): String? = withContext(Dispatchers.IO) {
        runCatching {
            if (forceRefresh) {
                ConfigRemoteManager.refresh()
            }
            val configKey = NewsLanguage.remoteConfigKey(options.remoteConfigKey, languageKey)
            ConfigRemoteManager.getString(configKey, "")
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
