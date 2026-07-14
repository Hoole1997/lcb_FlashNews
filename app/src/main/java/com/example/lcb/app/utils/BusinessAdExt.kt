package com.example.lcb.app.utils

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.android.common.bill.ads.AdResult
import com.android.common.bill.ads.ext.AdShowExt
import com.android.common.bill.ui.NativeAdStyleType
import com.example.lcb.app.LcbApp
import kotlinx.coroutines.launch

/** 业务广告场景名集中维护，保证广告平台按真实入口区分数据。 */
object NativeAdPosition {
    const val HOME_FEED = "home_feed"
    const val FAVORITES_FEED = "favorites_feed"
    const val ME_SETTINGS = "me_settings"

    private const val MB_REWARD_VIDEO_ACTIVITY =
        "com.mbridge.msdk.reward.player.MBRewardVideoActivity"
    private const val CHARTBOOST_IMPRESSION_ACTIVITY =
        "com.chartboost.sdk.view.CBImpressionActivity"
    private const val UNITY_FULLSCREEN_WEB_VIEW_ACTIVITY =
        "com.unity3d.ads.adplayer.FullScreenWebViewDisplay"

    private const val FIX_MTG = "fix_mtg"
    private const val STOP_CHARTBOOST_FIX = "stop_chartboost_fix"
    private const val STOP_UNITY_FIX = "stop_unity_fix"
}

fun FragmentActivity.loadNative(
    container: ViewGroup,
    styleType: NativeAdStyleType = NativeAdStyleType.STANDARD,
    condition: () -> Boolean = { true },
    call: (Boolean) -> Unit = {},
    position: String? = null
) {
    lifecycleScope.launch {
        try {
            if (!condition.invoke()) {
                container.visibility = View.GONE
                call.invoke(false)
                return@launch
            }

            val success = AdShowExt.showNativeAdInContainer(
                context = container.context,
                container = container,
                styleType = styleType,
                position = position
            )

            if (success) {
                container.visibility = View.VISIBLE
                call.invoke(true)
            } else {
                container.visibility = View.GONE
                call.invoke(false)
            }
        } catch (_: Exception) {
            container.visibility = View.GONE
            call.invoke(false)
        }
    }
}

fun FragmentActivity.loadInterstitial(
    condition: () -> Boolean = { true },
    call: (Boolean) -> Unit
) {
    lifecycleScope.launch {
        try {
            if (!condition.invoke()) {
                call.invoke(false)
                return@launch
            }
            LcbApp.fixAdBug(activity = this@loadInterstitial,"")
            when (AdShowExt.showInterstitialAd(this@loadInterstitial)) {
                is AdResult.Success -> call.invoke(true)
                is AdResult.Failure -> call.invoke(false)
            }
        } catch (_: Exception) {
            call.invoke(false)
        }
    }
}
