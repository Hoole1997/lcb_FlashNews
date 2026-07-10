package com.example.lcb.news.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.lcb.news.data.NewsRepository

class NewsLifecycleBinding(
    private val owner: LifecycleOwner,
    private val repository: NewsRepository,
) : DefaultLifecycleObserver, NewsLifecycleHandle {
    private var bound = true

    init {
        owner.lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        // 页面不可见时收缩最近新闻缓存，保留聚合缓存，返回首页时仍能快速恢复。
        repository.trimMemory()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        unbind()
    }

    override fun unbind() {
        if (!bound) return
        bound = false
        owner.lifecycle.removeObserver(this)
    }
}
