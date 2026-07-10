package com.example.lcb.app.news.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.animation.ValueAnimator
import android.view.animation.PathInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lcb.app.R
import com.example.lcb.app.databinding.FragmentFlashNewsCategoryBinding
import com.example.lcb.app.utils.NativeAdPosition
import com.example.lcb.app.utils.loadNative
import com.example.lcb.news.api.NewsSdk
import com.example.lcb.news.lifecycle.NewsLifecycleHandle
import kotlinx.coroutines.launch

class NewsCategoryFragment : Fragment() {
    private var _binding: FragmentFlashNewsCategoryBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: NewsCategoryViewModel by viewModels()
    private lateinit var newsAdapter: NewsCardAdapter
    private var lifecycleHandle: NewsLifecycleHandle? = null
    private var pendingScrollRestore = true
    private var loadVisibleAdsRunnable: Runnable? = null
    private var initialLoadingObserved = false
    private var initialContentRevealed = false
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dy <= 0) return
            val manager = recyclerView.layoutManager as? LinearLayoutManager ?: return
            val lastVisible = manager.findLastVisibleItemPosition()
            if (lastVisible >= newsAdapter.itemCount - LOAD_MORE_THRESHOLD) {
                viewModel.loadMore(NewsSdk.client(requireContext()))
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                scheduleVisibleAdLoad()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFlashNewsCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val client = NewsSdk.client(requireContext())
        viewModel.bindCategory(requireArguments().getString(ARG_CATEGORY_KEY).orEmpty())
        lifecycleHandle = client.bind(viewLifecycleOwner)
        newsAdapter = NewsCardAdapter(
            onNewsClick = { item ->
                (activity as? FlashNewsNavigator)?.openNewsDetail(item)
            },
            isFavorite = { newsId -> client.isFavorite(newsId) },
            onFavoriteClick = { item ->
                viewLifecycleOwner.lifecycleScope.launch {
                    client.toggleFavorite(item)
                    newsAdapter.notifyArticleChanged(item.id)
                }
            },
            loadNativeAd = { container, callback ->
                activity?.loadNative(
                    container = container,
                    call = callback,
                    position = NativeAdPosition.HOME_FEED,
                ) ?: callback(false)
            },
        )
        binding.newsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.newsRecycler.adapter = newsAdapter
        binding.newsRecycler.setItemViewCacheSize(8)
        NewsListMotion.attach(binding.newsRecycler)
        binding.newsRecycler.addOnScrollListener(scrollListener)
        binding.swipeRefresh.setColorSchemeResources(R.color.flash_red)
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh(client)
        }
        collectUiState()
        collectPublishedTimeTicks()
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (_binding != null) {
                        renderState(state)
                    }
                }
            }
        }
    }

    private fun collectPublishedTimeTicks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                NewsPublishedTimeTicker.ticks.collect {
                    _binding?.newsRecycler?.let(newsAdapter::refreshVisiblePublishedTimes)
                }
            }
        }
    }

    private fun renderState(state: NewsCategoryUiState) {
        binding.swipeRefresh.isRefreshing = state.refreshing
        val showInitialLoading = state.items.isEmpty() && !state.loadedOnce
        if (showInitialLoading) {
            initialLoadingObserved = true
            showInitialLoadingState()
        }
        val showLoadingMore = state.loading && state.items.isNotEmpty() && !state.refreshing
        if (!newsAdapter.displaysState(state.items, showLoadingMore)) {
            newsAdapter.submitArticles(state.items, showLoadingMore = showLoadingMore) {
                restoreScrollIfNeeded()
                scheduleVisibleAdLoad()
                revealInitialContentIfReady(showInitialLoading)
            }
        } else {
            restoreScrollIfNeeded()
            scheduleVisibleAdLoad()
            revealInitialContentIfReady(showInitialLoading)
        }
    }

    private fun showInitialLoadingState() {
        if (initialContentRevealed) return
        binding.swipeRefresh.isEnabled = false
        binding.initialLoading.apply {
            animate().cancel()
            alpha = 1f
            visibility = View.VISIBLE
        }
        binding.newsRecycler.apply {
            animate().cancel()
            alpha = 0f
            visibility = View.INVISIBLE
        }
    }

    private fun revealInitialContentIfReady(showInitialLoading: Boolean) {
        if (showInitialLoading || initialContentRevealed) return
        initialContentRevealed = true
        binding.swipeRefresh.isEnabled = true

        val shouldAnimate = initialLoadingObserved && ValueAnimator.areAnimatorsEnabled()
        if (!shouldAnimate) {
            binding.initialLoading.visibility = View.GONE
            binding.newsRecycler.alpha = 1f
            binding.newsRecycler.translationY = 0f
            binding.newsRecycler.visibility = View.VISIBLE
            return
        }

        val enterDistance = resources.displayMetrics.density * 8f
        binding.newsRecycler.apply {
            visibility = View.VISIBLE
            alpha = 0f
            translationY = enterDistance
            animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(55L)
                .setDuration(280L)
                .setInterpolator(PathInterpolator(0.16f, 1f, 0.3f, 1f))
                .start()
        }
        binding.initialLoading.animate()
            .alpha(0f)
            .setDuration(170L)
            .setInterpolator(PathInterpolator(0.25f, 1f, 0.5f, 1f))
            .withEndAction {
                if (_binding != null) binding.initialLoading.visibility = View.GONE
            }
            .start()
    }

    private fun scheduleVisibleAdLoad() {
        val recyclerView = _binding?.newsRecycler ?: return
        loadVisibleAdsRunnable?.let(recyclerView::removeCallbacks)
        loadVisibleAdsRunnable = Runnable {
            if (_binding != null) {
                newsAdapter.loadVisibleNativeAds(recyclerView)
            }
        }
        recyclerView.postDelayed(loadVisibleAdsRunnable, LOAD_NATIVE_AD_AFTER_IDLE_MS)
    }

    private fun restoreScrollIfNeeded() {
        if (!pendingScrollRestore || newsAdapter.itemCount == 0) return
        val manager = binding.newsRecycler.layoutManager as? LinearLayoutManager ?: return
        manager.scrollToPositionWithOffset(viewModel.savedScrollPosition, viewModel.savedScrollOffset)
        pendingScrollRestore = false
    }

    private fun saveRecyclerScroll() {
        val manager = binding.newsRecycler.layoutManager as? LinearLayoutManager ?: return
        val position = manager.findFirstVisibleItemPosition().coerceAtLeast(0)
        val top = manager.findViewByPosition(position)?.top ?: binding.newsRecycler.paddingTop
        viewModel.saveScroll(position, top - binding.newsRecycler.paddingTop)
    }

    override fun onResume() {
        super.onResume()
        if (::newsAdapter.isInitialized) {
            // ViewPager 会预创建相邻页；等页面真正可见再加载，避免用户未访问的 Tab 请求 RSS。
            viewModel.loadInitial(NewsSdk.client(requireContext()))
            newsAdapter.notifyFavoriteStatesChanged()
        }
    }

    override fun onPause() {
        if (_binding != null) {
            saveRecyclerScroll()
        }
        super.onPause()
    }

    override fun onDestroyView() {
        saveRecyclerScroll()
        loadVisibleAdsRunnable?.let(binding.newsRecycler::removeCallbacks)
        loadVisibleAdsRunnable = null
        binding.newsRecycler.removeOnScrollListener(scrollListener)
        binding.newsRecycler.adapter = null
        lifecycleHandle?.unbind()
        lifecycleHandle = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_CATEGORY_KEY = "category_key"
        private const val LOAD_MORE_THRESHOLD = 4
        private const val LOAD_NATIVE_AD_AFTER_IDLE_MS = 120L

        fun newInstance(categoryKey: String): NewsCategoryFragment {
            return NewsCategoryFragment().apply {
                arguments = Bundle().apply { putString(ARG_CATEGORY_KEY, categoryKey) }
            }
        }
    }
}
