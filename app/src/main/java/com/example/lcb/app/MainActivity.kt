package com.example.lcb.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.lcb.app.databinding.ActivityMainHomeBinding
import com.example.lcb.app.news.ui.FavoritesFragment
import com.example.lcb.app.news.ui.FlashNewsNavigator
import com.example.lcb.app.news.ui.HomeFragment
import com.example.lcb.app.news.ui.MeFragment
import com.example.lcb.app.news.ui.NewsDetailActivity
import com.example.lcb.news.api.NewsSdk
import com.example.lcb.news.model.NewsArticle

class MainActivity : AppCompatActivity(), FlashNewsNavigator {
    private lateinit var binding: ActivityMainHomeBinding
    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            val tab = MainTab.fromPosition(position)
            if (binding.bottomNavigation.selectedItemId != tab.itemId) {
                binding.bottomNavigation.selectedItemId = tab.itemId
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mainPager.adapter = MainPagerAdapter(this)
        binding.mainPager.offscreenPageLimit = MainTab.values().lastIndex
        binding.mainPager.isUserInputEnabled = false
        binding.mainPager.registerOnPageChangeCallback(pageChangeCallback)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val tab = MainTab.fromItemId(item.itemId) ?: return@setOnItemSelectedListener false
            if (binding.mainPager.currentItem != tab.position) {
                binding.mainPager.setCurrentItem(tab.position, false)
            }
            true
        }

        val initialPage = savedInstanceState?.getInt(KEY_MAIN_PAGE) ?: MainTab.HOME.position
        binding.mainPager.setCurrentItem(initialPage.coerceIn(0, MainTab.values().lastIndex), false)
        binding.bottomNavigation.selectedItemId = MainTab.fromPosition(binding.mainPager.currentItem).itemId
    }

    override fun openNewsDetail(article: NewsArticle) {
        // 详情页按 id 查询数据；先记录用户实际点击的对象，避免读取到刷新前的不完整缓存。
        NewsSdk.client(this).rememberArticle(article)
        startActivity(NewsDetailActivity.intent(this, article))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_MAIN_PAGE, binding.mainPager.currentItem)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        binding.mainPager.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroy()
    }

    private class MainPagerAdapter(activity: MainActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = MainTab.values().size

        override fun createFragment(position: Int): Fragment {
            return MainTab.fromPosition(position).createFragment()
        }

        override fun getItemId(position: Int): Long {
            return MainTab.fromPosition(position).stableId
        }

        override fun containsItem(itemId: Long): Boolean {
            return MainTab.values().any { it.stableId == itemId }
        }
    }

    private enum class MainTab(val position: Int, val itemId: Int, val stableId: Long) {
        HOME(0, R.id.nav_home, 10L),
        FAVORITES(1, R.id.nav_favorites, 20L),
        ME(2, R.id.nav_me, 30L);

        fun createFragment(): Fragment {
            return when (this) {
                HOME -> HomeFragment()
                FAVORITES -> FavoritesFragment()
                ME -> MeFragment()
            }
        }

        companion object {
            fun fromItemId(itemId: Int): MainTab? {
                return values().firstOrNull { it.itemId == itemId }
            }

            fun fromPosition(position: Int): MainTab {
                return values().firstOrNull { it.position == position } ?: HOME
            }
        }
    }

    override fun onBackPressed() {
        LcbApp.backLaunchActivity()
    }

    private companion object {
        private const val KEY_MAIN_PAGE = "main_page"
    }
}
