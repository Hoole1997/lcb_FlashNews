package com.example.lcb.news.config

import android.content.Context
import com.example.lcb.news.model.NewsSdkOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssetNewsConfigDataSource(
    context: Context,
    private val options: NewsSdkOptions,
    private val languageKey: String,
) : NewsConfigDataSource {
    private val appContext = context.applicationContext

    override suspend fun read(forceRefresh: Boolean): String? = withContext(Dispatchers.IO) {
        val assetNames = buildList {
            if (languageKey != NewsLanguage.DEFAULT) {
                add(NewsLanguage.localizedAssetName(languageKey))
            }
            add(options.defaultAssetName)
        }
        assetNames.firstNotNullOfOrNull { assetName ->
            runCatching {
                appContext.assets.open(assetName).bufferedReader().use { it.readText() }
            }.getOrNull()
        }
    }
}
