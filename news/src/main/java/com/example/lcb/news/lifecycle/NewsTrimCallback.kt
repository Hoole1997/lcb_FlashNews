package com.example.lcb.news.lifecycle

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import com.example.lcb.news.data.NewsRepository

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class NewsTrimCallback(
    private val repository: NewsRepository,
) : ComponentCallbacks2 {

    override fun onTrimMemory(level: Int) {
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> repository.clearMemory()
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> repository.trimMemory()
        }
    }

    override fun onLowMemory() {
        repository.clearMemory()
    }

    override fun onConfigurationChanged(newConfig: Configuration) = Unit
}
