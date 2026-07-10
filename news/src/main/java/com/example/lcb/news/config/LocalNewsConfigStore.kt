package com.example.lcb.news.config

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalNewsConfigStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    suspend fun readLastGood(languageKey: String): String? = withContext(Dispatchers.IO) {
        prefs.getString(storageKey(languageKey), null)?.takeIf { it.isNotBlank() }
    }

    suspend fun saveLastGood(languageKey: String, json: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(storageKey(languageKey), json).apply()
    }

    private fun storageKey(languageKey: String): String {
        return "${KEY_LAST_GOOD_JSON}_$languageKey"
    }

    private companion object {
        const val PREF_NAME = "news_sdk_config"
        const val KEY_LAST_GOOD_JSON = "last_good_json"
    }
}
