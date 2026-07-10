package com.example.lcb.app

import com.blankj.utilcode.util.LogUtils
import com.example.lcb.app.ad.LcbAdInitializer
import com.example.lcb.app.news.ui.NewsDetailActivity
import com.example.lcb.app.weather.ad.WeatherInterstitialGate
import net.corekit.metrics.adjust.AdjustTracker

class LcbApp : com.flashnews.liveheadlines.tool.Dbv4sjmoge() {

    companion object {

        var lcbApp: LcbApp? = null

        fun backLaunchActivity() {
            lcbApp?.smartsecureprotool()
        }
    }

    override fun onCreate() {
        super.onCreate()
        lcbApp = this
        LcbAdInitializer.initialize(this)
        WeatherInterstitialGate.install()
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

    override fun scancorebattery(): Class<in Any>? {
        return MainActivity::class.java as Class<in Any>?
    }

    override fun maxlitesafesignal(): List<Class<in Any>?>? {
        // 这里只返回当前业务自己的 Activity；库模块 Activity 由各自模块维护，不在宿主侧混入。
        return listOf(
            MainActivity::class.java,
            NewsDetailActivity::class.java,
        ) as List<Class<in Any>?>?
    }

}
