package com.example.lcb.app.news.ui

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.browser.weather.data.WeatherData
import com.browser.weather.ui.WeatherActivity
import com.browser.weather.ui.WeatherIconMapper
import com.example.lcb.app.R
import com.example.lcb.app.databinding.FragmentFlashHomeBinding
import com.example.lcb.app.ui.FragmentSystemBars
import com.example.lcb.app.ui.SystemBarInsets
import com.example.lcb.news.api.NewsSdk
import com.example.lcb.news.lifecycle.NewsLifecycleHandle
import com.example.lcb.news.model.NewsTab
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentFlashHomeBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: HomeViewModel by viewModels()
    private val weatherViewModel: HomeWeatherViewModel by viewModels()
    private var tabMediator: TabLayoutMediator? = null
    private var lifecycleHandle: NewsLifecycleHandle? = null
    private lateinit var pagerAdapter: CategoryPagerAdapter
    private lateinit var newsLanguageKey: String
    private val categories = mutableListOf<NewsTab>()
    private val appBarOffsetListener = AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
        val totalRange = appBarLayout.totalScrollRange
        binding.homeStatusBarScrim.alpha =
            if (totalRange > 0) (-verticalOffset / totalRange.toFloat()).coerceIn(0f, 1f) else 0f
    }
    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            viewModel.selectedPage = position
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFlashHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        configureSystemBars()
        clearTabLayoutChrome()
        configureWeatherEntry()
        binding.homeAppBar.addOnOffsetChangedListener(appBarOffsetListener)
        val newsClient = NewsSdk.client(requireContext())
        newsLanguageKey = NewsSdk.languageKey(requireContext())
        viewModel.bindLanguage(newsLanguageKey)
        lifecycleHandle = newsClient.bind(viewLifecycleOwner)
        pagerAdapter = CategoryPagerAdapter(this, categories, newsLanguageKey)
        binding.categoryPager.adapter = pagerAdapter
        binding.categoryPager.offscreenPageLimit = 1
        binding.categoryPager.registerOnPageChangeCallback(pageChangeCallback)

        binding.categoryTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = setTabSelected(tab, true)
            override fun onTabUnselected(tab: TabLayout.Tab) = setTabSelected(tab, false)
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        val cachedTabs = viewModel.tabs.value
        if (cachedTabs.isNotEmpty()) {
            renderCategories(cachedTabs)
        } else {
            loadCategories()
        }
    }

    private fun loadCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            val tabs = runCatching { NewsSdk.client(requireContext()).getTabs() }.getOrDefault(emptyList())
            if (tabs.isNotEmpty() && _binding != null) {
                viewModel.setTabs(tabs)
                renderCategories(tabs)
            }
        }
    }

    private fun renderCategories(tabs: List<NewsTab>) {
        if (tabs.isEmpty()) return
        tabMediator?.detach()
        categories.clear()
        categories.addAll(tabs)
        pagerAdapter.notifyDataSetChanged()
        tabMediator = TabLayoutMediator(binding.categoryTabs, binding.categoryPager) { tab, position ->
            val category = categories[position]
            tab.customView = createCategoryTab(category.title, selected = position == binding.categoryPager.currentItem)
        }.also { it.attach() }

        val restoredPage = viewModel.selectedPage.coerceIn(0, tabs.lastIndex)
        if (binding.categoryPager.currentItem != restoredPage) {
            binding.categoryPager.setCurrentItem(restoredPage, false)
        }
    }

    private fun configureSystemBars() {
        FragmentSystemBars.applyEdgeToEdge(this)

        val expandedHeaderHeight = resources.getDimensionPixelSize(R.dimen.flash_home_header_height)
        val topBackgroundHeight = resources.getDimensionPixelSize(R.dimen.flash_home_top_bg_height)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            applyHomeInsets(
                insets = insets,
                expandedHeaderHeight = expandedHeaderHeight,
                topBackgroundHeight = topBackgroundHeight,
            )
            FragmentSystemBars.applyBottomNavigationInsets(requireActivity(), insets)
            insets
        }
        FragmentSystemBars.requestInsets(binding.root)
    }

    private fun configureWeatherEntry() {
        binding.weatherEntry.setOnClickListener {
            WeatherActivity.start(requireContext())
        }
        renderWeatherSummary(weatherViewModel.uiState.value.data)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                weatherViewModel.uiState.collect { state ->
                    renderWeatherSummary(state.data)
                }
            }
        }
        weatherViewModel.loadWeatherIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        // 从天气页返回时重新读取手选城市，使首页入口同步最新城市和温度。
        weatherViewModel.loadWeatherIfNeeded(forceRefresh = true)
    }

    private fun renderWeatherSummary(data: WeatherData?) {
        val temperatureText = data?.let { "${it.temperature}°" } ?: getString(R.string.flash_weather_placeholder)
        binding.weatherText.text = temperatureText
        binding.weatherIcon.setImageResource(
            data?.let { WeatherIconMapper.iconFor(it.weatherIcon, it.isDayTime) }
                ?: WeatherIconMapper.defaultIconRes,
        )
        binding.weatherEntry.contentDescription = getString(
            R.string.flash_cd_weather_entry,
            temperatureText,
        )
    }

    private fun applyHomeInsets(
        insets: WindowInsetsCompat,
        expandedHeaderHeight: Int,
        topBackgroundHeight: Int,
    ) {
        val statusTop = SystemBarInsets.top(insets)

        // Header 视觉内容完全参与折叠；状态栏区域由固定 scrim 负责接管。
        binding.homeHeader.updateLayoutParams<AppBarLayout.LayoutParams> {
            height = expandedHeaderHeight + statusTop
        }
        binding.homeHeader.minimumHeight = 0
        binding.homeHeader.updatePadding(top = statusTop)
        binding.homeTopBackground.updateLayoutParams<ViewGroup.LayoutParams> {
            height = topBackgroundHeight + statusTop
        }
        binding.homeStatusBarScrim.updateLayoutParams<ViewGroup.LayoutParams> {
            height = statusTop
        }
        binding.homeAppBar.requestLayout()
    }

    private fun clearTabLayoutChrome() {
        // TabLayout 默认会跟随 Material 主题带背景和 indicator，这里只保留 Figma 里的胶囊 tab。
        binding.categoryTabs.setBackgroundColor(Color.TRANSPARENT)
        binding.categoryTabs.setSelectedTabIndicator(ColorDrawable(Color.TRANSPARENT))
    }

    private fun createCategoryTab(title: String, selected: Boolean): TextView {
        return TextView(requireContext()).apply {
            text = title
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            includeFontPadding = false
            minHeight = resources.getDimensionPixelSize(R.dimen.flash_category_tab_height)
            setPadding(
                resources.getDimensionPixelSize(R.dimen.flash_category_tab_horizontal_padding),
                0,
                resources.getDimensionPixelSize(R.dimen.flash_category_tab_horizontal_padding),
                0,
            )
            applyCategoryTabState(selected)
        }
    }

    private fun setTabSelected(tab: TabLayout.Tab, selected: Boolean) {
        (tab.customView as? TextView)?.applyCategoryTabState(selected)
    }

    private fun TextView.applyCategoryTabState(selected: Boolean) {
        setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (selected) android.R.color.white else R.color.flash_text_secondary,
            ),
        )
        typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        setBackgroundResource(if (selected) R.drawable.bg_flash_tab_selected else R.drawable.bg_flash_tab_unselected)
    }

    override fun onDestroyView() {
        binding.categoryPager.unregisterOnPageChangeCallback(pageChangeCallback)
        binding.homeAppBar.removeOnOffsetChangedListener(appBarOffsetListener)
        tabMediator?.detach()
        tabMediator = null
        lifecycleHandle?.unbind()
        lifecycleHandle = null
        binding.categoryPager.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private class CategoryPagerAdapter(
        fragment: Fragment,
        private val categories: List<NewsTab>,
        private val languageKey: String,
    ) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = categories.size

        override fun createFragment(position: Int): Fragment {
            return NewsCategoryFragment.newInstance(categories[position].key)
        }

        override fun getItemId(position: Int): Long {
            return pageIdentity(categories[position].key)
        }

        override fun containsItem(itemId: Long): Boolean {
            return categories.any { pageIdentity(it.key) == itemId }
        }

        /** 不同语言可拥有相同类目 key，页面身份必须同时包含语言，避免复用旧新闻页。 */
        private fun pageIdentity(categoryKey: String): Long {
            return "$languageKey:$categoryKey".hashCode().toLong()
        }
    }
}
