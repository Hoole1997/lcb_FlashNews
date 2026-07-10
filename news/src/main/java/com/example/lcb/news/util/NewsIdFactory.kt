package com.example.lcb.news.util

import java.security.MessageDigest

internal object NewsIdFactory {
    fun fromUrl(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(url.trim().lowercase().toByteArray())
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }
}
