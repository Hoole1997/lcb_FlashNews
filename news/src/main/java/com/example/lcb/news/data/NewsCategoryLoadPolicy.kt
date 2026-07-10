package com.example.lcb.news.data

/**
 * 控制类目聚合数据的读取方式，区分首次加载、分页和用户主动刷新。
 */
enum class NewsCategoryLoadPolicy {
    /** 缓存未过期时复用，否则重新请求该类目下的 RSS。 */
    CACHE_WITH_TTL,

    /** 分页优先读取已有批次，读完后只请求下一个尚未加载的 RSS。 */
    NEXT_FEED,

    /** 用户主动刷新时忽略缓存并重新请求全部 RSS。 */
    FORCE_REFRESH,
}
