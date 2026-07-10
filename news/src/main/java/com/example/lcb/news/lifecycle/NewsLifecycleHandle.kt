package com.example.lcb.news.lifecycle

interface NewsLifecycleHandle : AutoCloseable {
    fun unbind()

    override fun close() {
        unbind()
    }
}
