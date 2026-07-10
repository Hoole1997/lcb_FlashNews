package com.example.lcb.news.config

import org.junit.Assert.assertEquals
import org.junit.Test

class NewsLanguageTest {
    @Test
    fun resolvesSupportedLanguageFromRegionalLocale() {
        assertEquals("es", NewsLanguage.fromLocaleTag("es-MX"))
        assertEquals("pt", NewsLanguage.fromLocaleTag("pt-BR"))
        assertEquals("zh-CN", NewsLanguage.fromLocaleTag("zh-Hans-CN"))
    }

    @Test
    fun fallsBackToEnglishForUnsupportedLanguage() {
        assertEquals("en", NewsLanguage.fromLocaleTag("ar-SA"))
        assertEquals("en", NewsLanguage.fromLocaleTag(null))
    }

    @Test
    fun createsIsolatedAssetAndRemoteConfigKeys() {
        assertEquals("news_feeds_zh_cn.json", NewsLanguage.localizedAssetName("zh-CN"))
        assertEquals("flash_news_feed_config_ja", NewsLanguage.remoteConfigKey("flash_news_feed_config", "ja"))
        assertEquals("flash_news_feed_config", NewsLanguage.remoteConfigKey("flash_news_feed_config", "en"))
    }
}
