package com.example.lcb.app.news.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.lcb.app.R

/**
 * “我的”页面外部跳转统一收口，避免 Fragment 直接关心 Intent 拼装细节。
 */
object MeExternalNavigator {
    private const val FEEDBACK_EMAIL = "biolumianescent@gmail.com"
    private const val PRIVACY_URL = "https://bioluminescents.com/privacy.html"

    fun openFeedback(context: Context): Boolean {
        val emailUri = Uri.fromParts("mailto", FEEDBACK_EMAIL, null)
        val emailIntent = Intent(Intent.ACTION_SENDTO, emailUri).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
            putExtra(
                Intent.EXTRA_SUBJECT,
                context.getString(R.string.flash_feedback_subject, context.getString(R.string.app_name)),
            )
        }
        val chooser = Intent.createChooser(
            emailIntent,
            context.getString(R.string.flash_feedback_chooser_title),
        )
        return launch(context, chooser)
    }

    fun openPrivacy(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        return launch(context, intent)
    }

    private fun launch(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
