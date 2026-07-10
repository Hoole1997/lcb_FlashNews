package com.example.lcb.app.news.ui

import androidx.recyclerview.widget.RecyclerView

/**
 * 列表动效统一入口，避免各页面直接关心 RecyclerView.ItemAnimator 的具体实现。
 */
object NewsListMotion {
    fun attach(recyclerView: RecyclerView) {
        recyclerView.itemAnimator = NewsListItemAnimator(recyclerView.context)
    }
}
