package com.example.lcb.app.news.ui

import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.lcb.app.R
import com.example.lcb.app.databinding.ItemFlashNewsCardBinding
import com.example.lcb.app.databinding.ItemFlashNewsLoadingMoreBinding
import com.example.lcb.app.databinding.ItemFlashNewsNativeAdBinding
import com.example.lcb.news.model.NewsArticle
import kotlin.math.PI
import kotlin.math.sin

class NewsCardAdapter(
    private val onNewsClick: (NewsArticle) -> Unit,
    private val isFavorite: (String) -> Boolean,
    private val onFavoriteClick: (NewsArticle) -> Unit,
    private val loadNativeAd: (ViewGroup, (Boolean) -> Unit) -> Unit,
) : ListAdapter<NewsFeedItem, RecyclerView.ViewHolder>(NewsDiffCallback) {

    private var submittedArticles: List<NewsArticle> = emptyList()
    private var submittedLoadingMore: Boolean = false

    /**
     * 广告位 id -> 已渲染广告 View。ViewHolder 回收后复用同一广告 View，避免滑回时重复请求。
     */
    private val nativeAdViewCache = mutableMapOf<Int, View>()
    private val requestedAdIds = mutableSetOf<Int>()

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is NewsFeedItem.News -> VIEW_TYPE_NEWS
            is NewsFeedItem.NativeAd -> VIEW_TYPE_NATIVE_AD
            NewsFeedItem.LoadingMore -> VIEW_TYPE_LOADING_MORE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_NATIVE_AD -> NativeAdViewHolder(
                ItemFlashNewsNativeAdBinding.inflate(inflater, parent, false),
                loadNativeAd,
                nativeAdViewCache,
                requestedAdIds,
            )
            VIEW_TYPE_LOADING_MORE -> LoadingMoreViewHolder(
                ItemFlashNewsLoadingMoreBinding.inflate(inflater, parent, false),
            )
            else -> NewsViewHolder(
                ItemFlashNewsCardBinding.inflate(inflater, parent, false),
                onNewsClick,
                isFavorite,
                onFavoriteClick,
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is NewsViewHolder -> {
                val item = getItem(position) as? NewsFeedItem.News ?: return
                holder.bind(item.article)
            }
            is NativeAdViewHolder -> {
                val item = getItem(position) as? NewsFeedItem.NativeAd ?: return
                holder.bind(item.id)
            }
            is LoadingMoreViewHolder -> holder.bind()
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        val item = getItem(position)
        if (payloads.isNotEmpty() && holder is NewsViewHolder && item is NewsFeedItem.News) {
            if (payloads.contains(PAYLOAD_FAVORITE_CHANGED)) holder.bindFavoriteState(item.article.id)
            if (payloads.contains(PAYLOAD_PUBLISHED_TIME_CHANGED)) holder.bindPublishedTime(item.article)
            return
        }
        onBindViewHolder(holder, position)
    }

    fun displaysState(articles: List<NewsArticle>, showLoadingMore: Boolean): Boolean {
        return submittedArticles == NewsAdInsertionPolicy.uniqueArticles(articles) &&
            submittedLoadingMore == showLoadingMore
    }

    fun displaysArticles(articles: List<NewsArticle>): Boolean {
        return displaysState(articles, showLoadingMore = false)
    }

    fun submitArticles(
        newItems: List<NewsArticle>,
        showLoadingMore: Boolean = false,
        appendTailAdWhenShort: Boolean = false,
        commitCallback: (() -> Unit)? = null,
    ) {
        submittedArticles = NewsAdInsertionPolicy.uniqueArticles(newItems)
        submittedLoadingMore = showLoadingMore
        val feedItems = NewsAdInsertionPolicy.buildFeedItems(
            articles = submittedArticles,
            appendTailAdWhenShort = appendTailAdWhenShort,
        ).toMutableList()
        if (showLoadingMore) {
            feedItems += NewsFeedItem.LoadingMore
        }
        submitList(
            feedItems,
            commitCallback,
        )
    }

    fun notifyArticleChanged(newsId: String) {
        val index = currentList.indexOfFirst { item ->
            item is NewsFeedItem.News && item.article.id == newsId
        }
        if (index >= 0) notifyItemChanged(index, PAYLOAD_FAVORITE_CHANGED)
    }

    fun notifyFavoriteStatesChanged() {
        if (itemCount > 0) notifyItemRangeChanged(0, itemCount, PAYLOAD_FAVORITE_CHANGED)
    }

    fun loadVisibleNativeAds(recyclerView: RecyclerView) {
        val layoutManager = recyclerView.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) return

        for (position in firstVisible..lastVisible) {
            if (currentList.getOrNull(position) is NewsFeedItem.NativeAd) {
                loadAdAtPosition(recyclerView, position)
            }
        }
    }

    fun loadAdAtPosition(recyclerView: RecyclerView, position: Int) {
        val holder = recyclerView.findViewHolderForAdapterPosition(position)
        if (holder is NativeAdViewHolder) {
            holder.loadAd()
        }
    }

    /** 只刷新当前屏幕内新闻的时间字段，避免分钟更新触发整表重绑。 */
    fun refreshVisiblePublishedTimes(recyclerView: RecyclerView) {
        val layoutManager = recyclerView.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) return
        for (position in firstVisible..lastVisible) {
            if (currentList.getOrNull(position) is NewsFeedItem.News) {
                notifyItemChanged(position, PAYLOAD_PUBLISHED_TIME_CHANGED)
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        nativeAdViewCache.values.forEach { cachedView ->
            (cachedView.parent as? ViewGroup)?.removeView(cachedView)
        }
        nativeAdViewCache.clear()
        requestedAdIds.clear()
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        (holder as? LoadingMoreViewHolder)?.startAnimation()
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        (holder as? LoadingMoreViewHolder)?.stopAnimation()
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        (holder as? LoadingMoreViewHolder)?.stopAnimation()
        super.onViewRecycled(holder)
    }

    class NewsViewHolder(
        private val binding: ItemFlashNewsCardBinding,
        private val onNewsClick: (NewsArticle) -> Unit,
        private val isFavorite: (String) -> Boolean,
        private val onFavoriteClick: (NewsArticle) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NewsArticle) {
            binding.newsTitle.text = item.title
            binding.sourceText.text = item.source
            bindPublishedTime(item)
            val logoModel: Any = item.sourceLogoUrl.takeIf { it.isNotBlank() } ?: R.drawable.img_news_source_wand
            loadCover(item.imageUrl)
            Glide.with(binding.sourceAvatar)
                .load(logoModel)
                .circleCrop()
                .placeholder(R.drawable.img_news_source_wand)
                .error(R.drawable.img_news_source_wand)
                .into(binding.sourceAvatar)
            bindFavoriteState(item.id)

            binding.root.setOnClickListener { onNewsClick(item) }
            binding.bookmarkButton.setOnClickListener {
                onFavoriteClick(item)
                // 收藏反馈保持轻量，避免列表滚动时触发昂贵动画。
                binding.bookmarkButton.animate()
                    .scaleX(1.18f)
                    .scaleY(1.18f)
                    .setDuration(90)
                    .withEndAction {
                        binding.bookmarkButton.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
                    }
                    .start()
            }
        }

        private fun loadCover(imageUrl: String) {
            if (imageUrl.isBlank()) {
                Glide.with(binding.newsImage).clear(binding.newsImage)
                binding.newsImage.setImageDrawable(null)
                binding.newsImageFrame.visibility = View.GONE
                binding.newsDefaultCover.visibility = View.GONE
                return
            }

            binding.newsImageFrame.visibility = View.VISIBLE
            binding.newsImage.visibility = View.VISIBLE
            binding.newsDefaultCover.visibility = View.VISIBLE
            Glide.with(binding.newsImage)
                .load(imageUrl)
                .dontAnimate()
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean,
                    ): Boolean {
                        binding.newsImage.setImageDrawable(null)
                        binding.newsImageFrame.visibility = View.GONE
                        binding.newsDefaultCover.visibility = View.GONE
                        return true
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean,
                    ): Boolean {
                        binding.newsDefaultCover.visibility = View.GONE
                        return false
                    }
                })
                .into(binding.newsImage)
        }

        fun bindFavoriteState(newsId: String) {
            val context = binding.root.context
            val favorite = isFavorite(newsId)
            binding.bookmarkButton.setImageResource(
                if (favorite) R.drawable.ic_flash_bookmark else R.drawable.ic_flash_bookmark_outline,
            )
            binding.bookmarkButton.setColorFilter(
                ContextCompat.getColor(context, if (favorite) R.color.flash_red else R.color.flash_text_primary),
            )
        }

        fun bindPublishedTime(article: NewsArticle) {
            binding.dateText.text = NewsPublishedTimeFormatter.format(binding.root.context, article)
        }
    }

    private class NativeAdViewHolder(
        private val binding: ItemFlashNewsNativeAdBinding,
        private val loadNativeAd: (ViewGroup, (Boolean) -> Unit) -> Unit,
        private val viewCache: MutableMap<Int, View>,
        private val requestedAdIds: MutableSet<Int>,
    ) : RecyclerView.ViewHolder(binding.root) {

        private var boundAdId: Int? = null

        fun bind(adId: Int) {
            if (boundAdId == adId && binding.adContainer.childCount > 0) return
            boundAdId = adId
            binding.adContainer.visibility = View.VISIBLE
            binding.adContainer.removeAllViews()

            val cachedView = viewCache[adId]
            when {
                cachedView != null -> {
                    (cachedView.parent as? ViewGroup)?.removeView(cachedView)
                    binding.adContainer.addView(cachedView)
                }
                requestedAdIds.contains(adId) -> {
                    binding.adContainer.visibility = View.GONE
                }
            }
        }

        fun loadAd() {
            val adId = boundAdId ?: return
            if (requestedAdIds.contains(adId)) return
            requestedAdIds.add(adId)

            loadNativeAd(binding.adContainer) { success ->
                if (success) {
                    binding.adContainer.getChildAt(0)?.let { renderedView ->
                        viewCache[adId] = renderedView
                    }
                } else {
                    viewCache.remove(adId)
                }
            }
        }
    }

    private class LoadingMoreViewHolder(
        private val binding: ItemFlashNewsLoadingMoreBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        private val inkMarks = listOf(binding.loadingMarkOne, binding.loadingMarkTwo, binding.loadingMarkThree)
        private var pulseAnimator: ValueAnimator? = null
        private var entered = false

        fun bind() {
            resetInkMarks()
        }

        fun startAnimation() {
            if (!ValueAnimator.areAnimatorsEnabled()) {
                binding.root.alpha = 1f
                binding.root.translationY = 0f
                resetInkMarks()
                return
            }

            if (!entered) {
                entered = true
                binding.root.alpha = 0f
                binding.root.translationY = binding.root.resources.displayMetrics.density * 8f
                binding.root.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(240L)
                    .setInterpolator(PathInterpolator(0.16f, 1f, 0.3f, 1f))
                    .withEndAction(::startPulse)
                    .start()
            } else {
                startPulse()
            }
        }

        fun stopAnimation() {
            binding.root.animate().cancel()
            pulseAnimator?.cancel()
            pulseAnimator = null
        }

        private fun startPulse() {
            if (pulseAnimator?.isRunning == true || !ValueAnimator.areAnimatorsEnabled()) return
            pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 900L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener { animator ->
                    val phase = animator.animatedValue as Float
                    inkMarks.forEachIndexed { index, mark ->
                        val wave = ((sin((phase - index * 0.16f) * 2f * PI) + 1f) / 2f).toFloat()
                        mark.alpha = 0.3f + wave * 0.7f
                        mark.scaleY = 0.72f + wave * 0.28f
                        mark.translationY = -mark.resources.displayMetrics.density * 2f * wave
                    }
                }
                start()
            }
        }

        private fun resetInkMarks() {
            inkMarks.forEachIndexed { index, mark ->
                mark.alpha = 0.45f + index * 0.2f
                mark.scaleY = 1f
                mark.translationY = 0f
            }
        }
    }

    private object NewsDiffCallback : DiffUtil.ItemCallback<NewsFeedItem>() {
        override fun areItemsTheSame(oldItem: NewsFeedItem, newItem: NewsFeedItem): Boolean {
            return when {
                oldItem is NewsFeedItem.News && newItem is NewsFeedItem.News ->
                    articleIdentity(oldItem.article) == articleIdentity(newItem.article)
                oldItem is NewsFeedItem.NativeAd && newItem is NewsFeedItem.NativeAd ->
                    oldItem.id == newItem.id
                oldItem === NewsFeedItem.LoadingMore && newItem === NewsFeedItem.LoadingMore -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: NewsFeedItem, newItem: NewsFeedItem): Boolean {
            return oldItem == newItem
        }
    }

    private companion object {
        private const val VIEW_TYPE_NEWS = 1
        private const val VIEW_TYPE_NATIVE_AD = 2
        private const val VIEW_TYPE_LOADING_MORE = 3
        private const val PAYLOAD_FAVORITE_CHANGED = "payload_favorite_changed"
        private const val PAYLOAD_PUBLISHED_TIME_CHANGED = "payload_published_time_changed"

        private fun articleIdentity(article: NewsArticle): String {
            return article.id.ifBlank { article.url }
        }
    }
}
