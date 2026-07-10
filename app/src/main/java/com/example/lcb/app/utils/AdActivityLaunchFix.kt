package com.example.lcb.app.utils

import android.app.Activity
import android.content.Intent
import net.corekit.core.ext.autoEncryptIfNeeded
import net.corekit.core.log.CoreLogger
import net.corekit.core.utils.ConfigRemoteManager
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

/**
 * 部分广告 SDK 会使用 Application Context 启动全屏 Activity，导致启动 flag / task 栈异常。
 * 展示广告前暂存调用方 Activity，并由 Application.startActivity 转交给该 Activity 启动。
 */
object AdActivityLaunchFix {
    private const val LOG = "AdActivityLaunchFix"

    private const val MB_REWARD_VIDEO_ACTIVITY =
        "com.mbridge.msdk.reward.player.MBRewardVideoActivity"
    private const val CHARTBOOST_IMPRESSION_ACTIVITY =
        "com.chartboost.sdk.view.CBImpressionActivity"
    private const val UNITY_FULLSCREEN_WEB_VIEW_ACTIVITY =
        "com.unity3d.ads.adplayer.FullScreenWebViewDisplay"

    private const val FIX_MTG = "fix_mtg"
    private const val STOP_CHARTBOOST_FIX = "stop_chartboost_fix"
    private const val STOP_UNITY_FIX = "stop_unity_fix"

    private val launchActivity = AtomicReference<WeakReference<Activity>?>(null)

    fun register(activity: Activity) {
        launchActivity.set(WeakReference(activity))
    }

    fun unregister(activity: Activity) {
        while (true) {
            val current = launchActivity.get() ?: return
            if (current.get() !== activity) return
            if (launchActivity.compareAndSet(current, null)) return
        }
    }

    /** @return true 表示 intent 已经交给广告调用前的 Activity 启动。 */
    fun redirect(intent: Intent?): Boolean {
        val className = intent?.component?.className ?: return false
        val activity = launchActivity.get()?.get()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            CoreLogger.w("$LOG: no active launch Activity, use Application")
            return false
        }

        val network = when (className) {
            MB_REWARD_VIDEO_ACTIVITY -> {
                if (!remoteBoolean(activity, FIX_MTG, defaultValue = false)) return false
                "mtg"
            }

            CHARTBOOST_IMPRESSION_ACTIVITY -> {
                if (remoteString(activity, STOP_CHARTBOOST_FIX).isNotEmpty()) return false
                "chartboost"
            }

            UNITY_FULLSCREEN_WEB_VIEW_ACTIVITY -> {
                if (remoteString(activity, STOP_UNITY_FIX).isNotEmpty()) return false
                "unity3d"
            }

            else -> return false
        }

        val originalFlags = intent.flags
        return try {
            CoreLogger.w("$LOG: [$network] redirect $className to ${activity.javaClass.name}")
            intent.flags = 0
            activity.startActivity(intent)
            true
        } catch (e: RuntimeException) {
            intent.flags = originalFlags
            CoreLogger.w("$LOG: [$network] redirect failed, use Application", e)
            false
        }
    }

    /**
     * Application.startActivity 是同步入口，不能调用 ConfigRemoteManager 的 suspend getter。
     * 这里直接读取 Firebase Remote Config 已激活的内存值；未初始化时使用业务默认值。
     */
    private fun remoteBoolean(activity: Activity, key: String, defaultValue: Boolean): Boolean {
        val remoteConfig = ConfigRemoteManager.getFirebaseRemoteConfig() ?: return defaultValue
        return runCatching {
            remoteConfig.getBoolean(key.remoteConfigKey(activity))
        }.getOrElse { error ->
            CoreLogger.w("$LOG: read boolean config $key failed", error)
            defaultValue
        }
    }

    private fun remoteString(activity: Activity, key: String, defaultValue: String = ""): String {
        val remoteConfig = ConfigRemoteManager.getFirebaseRemoteConfig() ?: return defaultValue
        return runCatching {
            remoteConfig.getString(key.remoteConfigKey(activity))
        }.getOrElse { error ->
            CoreLogger.w("$LOG: read string config $key failed", error)
            defaultValue
        }
    }

    // ConfigRemoteManager 内部同样会按包名加密 key；直接使用明文 key 只能读到默认值。
    private fun String.remoteConfigKey(activity: Activity): String =
        autoEncryptIfNeeded(activity.applicationContext.packageName)
}
