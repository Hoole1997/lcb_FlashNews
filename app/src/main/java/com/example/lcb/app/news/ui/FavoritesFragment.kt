package com.example.lcb.app.news.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lcb.app.databinding.FragmentFlashFavoritesBinding
import com.example.lcb.app.ui.FragmentSystemBars
import com.example.lcb.app.utils.NativeAdPosition
import com.example.lcb.app.utils.loadNative
import com.example.lcb.news.api.NewsSdk
import com.example.lcb.news.lifecycle.NewsLifecycleHandle
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {
    private var _binding: FragmentFlashFavoritesBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: FavoritesViewModel by viewModels()
    private lateinit var adapter: NewsCardAdapter
    private var lifecycleHandle: NewsLifecycleHandle? = null
    private var pendingScrollRestore = true
    private var loadVisibleAdsRunnable: Runnable? = null
    private val scrollListener = object : RecyclerView.OnScrollListener() {
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
        _binding = FragmentFlashFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        configureSystemBars()

        lifecycleHandle = NewsSdk.client(requireContext()).bind(viewLifecycleOwner)
        val client = NewsSdk.client(requireContext())
        adapter = NewsCardAdapter(
            onNewsClick = { item -> (activity as? FlashNewsNavigator)?.openNewsDetail(item) },
            isFavorite = { newsId -> client.isFavorite(newsId) },
            onFavoriteClick = { item ->
                viewModel.toggleFavorite(client, item)
            },
            loadNativeAd = { container, callback ->
                activity?.loadNative(
                    container = container,
                    call = callback,
                    position = NativeAdPosition.FAVORITES_FEED,
                ) ?: callback(false)
            },
        )
        binding.favoritesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.favoritesRecycler.adapter = adapter
        binding.favoritesRecycler.setItemViewCacheSize(8)
        NewsListMotion.attach(binding.favoritesRecycler)
        binding.favoritesRecycler.addOnScrollListener(scrollListener)
        collectUiState()
        collectPublishedTimeTicks()
    }

    private fun configureSystemBars() {
        FragmentSystemBars.applyEdgeToEdge(this)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            FragmentSystemBars.applyTopInsetToFixedHeightView(binding.favoritesTitle, insets)
            FragmentSystemBars.applyBottomNavigationInsets(requireActivity(), insets)
            insets
        }
        FragmentSystemBars.requestInsets(binding.root)
    }

    override fun onResume() {
        super.onResume()
        viewModel.load(NewsSdk.client(requireContext()))
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
                    _binding?.favoritesRecycler?.let(adapter::refreshVisiblePublishedTimes)
                }
            }
        }
    }

    private fun renderState(state: FavoritesUiState) {
        binding.emptyState.visibility = if (state.items.isEmpty() && state.loadedOnce) View.VISIBLE else View.GONE
        if (!adapter.displaysArticles(state.items)) {
            adapter.submitArticles(
                newItems = state.items,
                appendTailAdWhenShort = true,
            ) {
                restoreScrollIfNeeded()
                scheduleVisibleAdLoad()
            }
        } else {
            restoreScrollIfNeeded()
            scheduleVisibleAdLoad()
        }
    }

    private fun scheduleVisibleAdLoad() {
        val recyclerView = _binding?.favoritesRecycler ?: return
        loadVisibleAdsRunnable?.let(recyclerView::removeCallbacks)
        loadVisibleAdsRunnable = Runnable {
            if (_binding != null) {
                adapter.loadVisibleNativeAds(recyclerView)
            }
        }
        recyclerView.postDelayed(loadVisibleAdsRunnable, LOAD_NATIVE_AD_AFTER_IDLE_MS)
    }

    private fun restoreScrollIfNeeded() {
        if (!pendingScrollRestore || adapter.itemCount == 0) return
        val manager = binding.favoritesRecycler.layoutManager as? LinearLayoutManager ?: return
        manager.scrollToPositionWithOffset(viewModel.savedScrollPosition, viewModel.savedScrollOffset)
        pendingScrollRestore = false
    }

    private fun saveRecyclerScroll() {
        val manager = binding.favoritesRecycler.layoutManager as? LinearLayoutManager ?: return
        val position = manager.findFirstVisibleItemPosition().coerceAtLeast(0)
        val top = manager.findViewByPosition(position)?.top ?: binding.favoritesRecycler.paddingTop
        viewModel.saveScroll(position, top - binding.favoritesRecycler.paddingTop)
    }

    override fun onPause() {
        if (_binding != null) {
            saveRecyclerScroll()
        }
        super.onPause()
    }

    override fun onDestroyView() {
        saveRecyclerScroll()
        loadVisibleAdsRunnable?.let(binding.favoritesRecycler::removeCallbacks)
        loadVisibleAdsRunnable = null
        binding.favoritesRecycler.removeOnScrollListener(scrollListener)
        binding.favoritesRecycler.adapter = null
        lifecycleHandle?.unbind()
        lifecycleHandle = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        private const val LOAD_NATIVE_AD_AFTER_IDLE_MS = 120L
    }
}
