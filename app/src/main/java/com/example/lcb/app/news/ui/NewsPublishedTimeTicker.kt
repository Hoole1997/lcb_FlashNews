package com.example.lcb.app.news.ui

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** 页面可见期间每分钟触发一次时间文案更新，不涉及网络请求。 */
object NewsPublishedTimeTicker {
    val ticks: Flow<Unit> = flow {
        while (true) {
            delay(60_000L)
            emit(Unit)
        }
    }
}
