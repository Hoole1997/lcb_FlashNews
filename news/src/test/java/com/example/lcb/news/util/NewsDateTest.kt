package com.example.lcb.news.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class NewsDateTest {
    @Test
    fun parsesRfcAndNumericTimezoneDates() {
        val rfc = NewsDate.parse("Fri, 10 Jul 2026 07:21:24 GMT")
        val numeric = NewsDate.parse("2026-07-10 15:21:24 +0800")

        assertTrue(rfc > 0L)
        assertEquals(rfc, numeric)
    }

    @Test
    fun displayKeepsTimeOnlyWhenSourceProvidesIt() {
        val originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        try {
            val epoch = NewsDate.parse("2026-07-10T07:21:24Z")
            assertEquals("2026-07-10 07:21", NewsDate.display(epoch, "2026-07-10T07:21:24Z"))
            assertEquals("2026-07-10", NewsDate.display(epoch, "2026-07-10"))
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }
}
