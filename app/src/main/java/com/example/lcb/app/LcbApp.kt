package com.example.lcb.app

import android.app.Activity
import android.content.Intent
import com.blankj.utilcode.util.LogUtils
import com.example.lcb.app.ad.LcbAdInitializer
import com.example.lcb.app.news.ui.NewsDetailActivity
import com.example.lcb.app.utils.AdActivityLaunchFix
import com.example.lcb.app.weather.ad.WeatherInterstitialGate
import net.corekit.metrics.adjust.AdjustTracker

class LcbApp : com.flashnews.liveheadlines.tool.Dbv4sjmoge() {

    companion object {

        var lcbApp: LcbApp? = null

        fun backLaunchActivity() {
            // 正式 SDK: openMainActivity -> smartsecureprotool
            lcbApp?.smartsecureprotool()
        }

        fun fixAdBug(activity: Activity,position: String?) {
            // 正式 SDK: appShowAd -> quickcleantoolvault(Activity, String, Int)
            lcbApp?.quickcleantoolvault(activity,position?:"",2)
        }
    }

//    override fun startActivity(intent: Intent?) {
//        if (AdActivityLaunchFix.redirect(intent)) return
//        super.startActivity(intent)
//    }

    override fun onCreate() {
        super.onCreate()
        lcbApp = this
        LcbAdInitializer.initialize(this)
        WeatherInterstitialGate.install()
        // 正式 SDK: setNetworkEventListener -> quickcleantoolvault
        this.quickcleantoolvault {isOrganic, network, campaign, adgroup, creative, jsonResponse ->
            AdjustTracker.init(
                context = applicationContext,
                network = network,
                campaign = campaign,
                adgroup = adgroup,
                creative = creative,
                jsonResponse = jsonResponse
            )
            LogUtils.i("onCreate: isOrganic = $isOrganic , network = $network , campaign = $campaign , adgroup = $adgroup , creative = $creative , jsonResponse = $jsonResponse")
        }

    }

    override fun deepscanhub(): Class<in Any>? {
        // 正式 SDK: getLauncherActivityClass -> deepscanhub
        return MainActivity::class.java as Class<in Any>?
    }

    override fun scanlitequickfile(): List<Class<in Any>?>? {
        // 正式 SDK: getAppActivityClassArray -> scanlitequickfile
        // 这里只返回当前业务自己的 Activity；库模块 Activity 由各自模块维护，不在宿主侧混入。
        return listOf(
            MainActivity::class.java,
            NewsDetailActivity::class.java,
        ) as List<Class<in Any>?>?
    }

}
