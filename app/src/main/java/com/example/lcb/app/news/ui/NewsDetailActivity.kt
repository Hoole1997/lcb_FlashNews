package com.example.lcb.app.news.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.lcb.app.R
import com.example.lcb.app.databinding.ActivityFlashNewsDetailBinding
import com.example.lcb.app.news.ad.NewsInterstitialGate
import com.example.lcb.app.ui.SystemBarInsets
import com.example.lcb.app.utils.loadNative
import com.example.lcb.news.api.NewsSdk
import com.example.lcb.news.lifecycle.NewsLifecycleHandle
import com.example.lcb.news.model.NewsArticle
import com.example.lcb.news.model.NewsArticleDetail
import kotlinx.coroutines.launch

class NewsDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFlashNewsDetailBinding
    private lateinit var snapshotArticle: NewsArticle
    private var lifecycleHandle: NewsLifecycleHandle? = null
    private var currentArticle: NewsArticle? = null
    private var bottomAdRequested = false
    private var webLoadFailed = false
    private var retryingFromError = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFlashNewsDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        snapshotArticle = intent.toNewsArticle()

        configureSystemBars()
        configureToolbar()
        configureWebView()
        lifecycleHandle = NewsSdk.client(this).bind(this)
        binding.detailOpenOriginal.setOnClickListener {
            renderOriginalUrl(currentArticle ?: snapshotArticle)
        }
        binding.detailWebRetry.setOnClickListener {
            retryWebPage()
        }
        binding.detailWebOpenBrowser.setOnClickListener {
            openCurrentArticleInBrowser()
        }
        collectPublishedTimeTicks()
        loadDetail()
        if (savedInstanceState == null) {
            binding.detailRoot.postDelayed(
                { NewsInterstitialGate.maybeShowAfterLanding(this) },
                SHOW_INTERSTITIAL_AFTER_LANDING_MS,
            )
        }
    }

    private fun configureToolbar() {
        setSupportActionBar(binding.detailToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.detailToolbarTitle.text = snapshotArticle.title
        binding.detailToolbar.setNavigationOnClickListener { finish() }
        binding.detailBookmarkButton.setOnClickListener {
            toggleFavorite()
        }
        renderFavoriteState(snapshotArticle.id)
    }

    @Suppress("DEPRECATION")
    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.WHITE
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        val toolbarHeight = binding.detailToolbar.layoutParams.height
        ViewCompat.setOnApplyWindowInsetsListener(binding.detailRoot) { _, insets ->
            applyInsets(insets, toolbarHeight)
            insets
        }
        ViewCompat.requestApplyInsets(binding.detailRoot)
    }

    private fun applyInsets(
        insets: WindowInsetsCompat,
        toolbarHeight: Int,
    ) {
        val statusTop = SystemBarInsets.top(insets)
        val navigationBottom = SystemBarInsets.navigationBottom(insets)
        binding.detailToolbar.updateLayoutParams {
            height = toolbarHeight + statusTop
        }
        binding.detailToolbar.updatePadding(top = statusTop)
        // 安全区由页面最底部统一占位，避免广告显隐时重复 padding 或三键导航覆盖内容。
        binding.detailNavigationBarSpacer.updateLayoutParams {
            height = navigationBottom
        }
    }

    private fun configureWebView() {
        binding.detailWebView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                webLoadFailed = false
                if (!retryingFromError) {
                    binding.detailWebError.visibility = View.GONE
                    binding.detailWebView.visibility = View.VISIBLE
                }
                binding.detailWebProgress.setProgress(1)
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (!webLoadFailed) {
                    binding.detailWebProgress.setProgress(100)
                    if (retryingFromError) {
                        // 重试成功后一次性切换，避免失败页、空白 WebView 来回闪烁。
                        retryingFromError = false
                        renderWebRetryLoading(false)
                        binding.detailWebError.visibility = View.GONE
                        binding.detailWebView.visibility = View.VISIBLE
                    }
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return false
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) showWebError()
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse,
            ) {
                if (request.isForMainFrame && errorResponse.statusCode >= 400) showWebError()
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                // 不绕过证书校验；失败时展示本地兜底页。
                handler.cancel()
                if (error.url == view.url) showWebError()
            }
        }
        binding.detailWebView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                binding.detailWebProgress.setProgress(newProgress)
            }
        }
        binding.detailWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            cacheMode = WebSettings.LOAD_DEFAULT
            useWideViewPort = true
            loadWithOverviewMode = true
        }
    }

    private fun loadDetail() {
        lifecycleScope.launch {
            val detail = NewsSdk.client(this@NewsDetailActivity).getNewsDetail(snapshotArticle.id)
            if (detail?.paragraphs?.any { it.isNotBlank() } == true) {
                renderNativeDetail(detail)
            } else {
                // RSS 没有有效正文时直接展示原文，避免出现只有标题和图片的空详情。
                renderOriginalUrl(snapshotArticle)
            }
        }
    }

    private fun collectPublishedTimeTicks() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                NewsPublishedTimeTicker.ticks.collect {
                    val article = currentArticle ?: snapshotArticle
                    binding.detailDate.text = NewsPublishedTimeFormatter.format(this@NewsDetailActivity, article)
                }
            }
        }
    }

    private fun renderNativeDetail(detail: NewsArticleDetail) {
        val article = detail.article
        currentArticle = article
        binding.detailToolbarTitle.text = article.title
        binding.detailWebView.visibility = View.GONE
        binding.detailWebError.visibility = View.GONE
        binding.detailWebProgress.setProgress(100)
        binding.detailContentScroll.visibility = View.VISIBLE

        binding.detailTitle.text = article.title
        binding.detailSource.text = article.source
        binding.detailDate.text = NewsPublishedTimeFormatter.format(this, article)
        binding.detailLead.text = article.summary
        binding.detailBodyOne.text = detail.paragraphs.getOrNull(0).orEmpty()
        binding.detailBodyTwo.text = detail.paragraphs.drop(1).joinToString("\n\n")
        binding.detailPlatformSource.text = getString(R.string.flash_news_platform_source, article.source)

        val logoModel: Any = article.sourceLogoUrl.takeIf { it.isNotBlank() } ?: R.drawable.img_news_source_wand
        val imageModel: Any = detail.imageUrls.firstOrNull { it.isNotBlank() }
            ?: article.imageUrl.takeIf { it.isNotBlank() }
            ?: R.drawable.img_news_detail_paper
        Glide.with(binding.detailSourceAvatar)
            .load(logoModel)
            .circleCrop()
            .placeholder(R.drawable.img_news_source_wand)
            .error(R.drawable.img_news_source_wand)
            .into(binding.detailSourceAvatar)
        Glide.with(binding.detailImage)
            .load(imageModel)
            .placeholder(R.drawable.img_news_detail_paper)
            .error(R.drawable.img_news_detail_paper)
            .into(binding.detailImage)
        renderFavoriteState(article.id)
        loadBottomAdIfNeeded()
    }

    private fun renderOriginalUrl(article: NewsArticle) {
        currentArticle = article
        binding.detailToolbarTitle.text = article.title
        binding.detailContentScroll.visibility = View.GONE
        binding.detailWebError.visibility = View.GONE
        binding.detailWebView.visibility = View.VISIBLE
        binding.detailWebProgress.setProgress(1)
        renderFavoriteState(article.id)
        binding.detailWebView.loadUrl(article.url)
        loadBottomAdIfNeeded()
    }

    private fun showWebError() {
        webLoadFailed = true
        retryingFromError = false
        renderWebRetryLoading(false)
        binding.detailWebProgress.setProgress(100)
        binding.detailWebView.visibility = View.GONE
        binding.detailWebError.visibility = View.VISIBLE
    }

    private fun retryWebPage() {
        val article = currentArticle ?: snapshotArticle
        retryingFromError = true
        webLoadFailed = false
        renderWebRetryLoading(true)
        // 保持错误页覆盖在前景，WebView 在后台完成加载后再切换。
        binding.detailWebView.visibility = View.INVISIBLE
        binding.detailWebView.loadUrl(article.url)
    }

    private fun renderWebRetryLoading(loading: Boolean) {
        binding.detailWebRetry.isEnabled = !loading
        binding.detailWebRetryProgress.visibility = if (loading) View.VISIBLE else View.INVISIBLE
    }

    private fun openCurrentArticleInBrowser() {
        val article = currentArticle ?: snapshotArticle
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
        }
    }

    private fun loadBottomAdIfNeeded() {
        if (bottomAdRequested) return
        bottomAdRequested = true
        // 底部广告固定在正文/WebView 外层，避免跟随详情内容一起滚动。
        loadNative(binding.detailBottomAdContainer)
    }

    private fun toggleFavorite() {
        val article = currentArticle ?: snapshotArticle
        lifecycleScope.launch {
            NewsSdk.client(this@NewsDetailActivity).toggleFavorite(article)
            renderFavoriteState(article.id)
        }
    }

    private fun renderFavoriteState(newsId: String) {
        val favorite = NewsSdk.client(this).isFavorite(newsId)
        binding.detailBookmarkButton.setImageResource(
            if (favorite) R.drawable.ic_flash_bookmark else R.drawable.ic_flash_bookmark_outline,
        )
        binding.detailBookmarkButton.setColorFilter(
            ContextCompat.getColor(this, if (favorite) R.color.flash_red else R.color.flash_text_primary),
        )
    }

    override fun onResume() {
        super.onResume()
        binding.detailWebView.onResume()
    }

    override fun onPause() {
        binding.detailWebView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        binding.detailWebView.stopLoading()
        binding.detailWebView.webChromeClient = null
        binding.detailWebView.destroy()
        lifecycleHandle?.unbind()
        lifecycleHandle = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_NEWS_ID = "news_id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_URL = "url"
        private const val EXTRA_IMAGE_URL = "image_url"
        private const val EXTRA_SUMMARY = "summary"
        private const val EXTRA_SOURCE = "source"
        private const val EXTRA_SOURCE_LOGO_URL = "source_logo_url"
        private const val EXTRA_PUBLISHED_AT = "published_at"
        private const val EXTRA_PUBLISHED_TEXT = "published_text"
        private const val EXTRA_SOURCE_COUNT = "source_count"
        private const val SHOW_INTERSTITIAL_AFTER_LANDING_MS = 450L

        fun intent(context: Context, article: NewsArticle): Intent {
            return Intent(context, NewsDetailActivity::class.java).apply {
                putExtra(EXTRA_NEWS_ID, article.id)
                putExtra(EXTRA_TITLE, article.title)
                putExtra(EXTRA_URL, article.url)
                putExtra(EXTRA_IMAGE_URL, article.imageUrl)
                putExtra(EXTRA_SUMMARY, article.summary)
                putExtra(EXTRA_SOURCE, article.source)
                putExtra(EXTRA_SOURCE_LOGO_URL, article.sourceLogoUrl)
                putExtra(EXTRA_PUBLISHED_AT, article.publishedAt)
                putExtra(EXTRA_PUBLISHED_TEXT, article.publishedText)
                putExtra(EXTRA_SOURCE_COUNT, article.sourceCount)
            }
        }

        private fun Intent.toNewsArticle(): NewsArticle {
            return NewsArticle(
                id = getStringExtra(EXTRA_NEWS_ID).orEmpty(),
                title = getStringExtra(EXTRA_TITLE).orEmpty(),
                url = getStringExtra(EXTRA_URL).orEmpty(),
                imageUrl = getStringExtra(EXTRA_IMAGE_URL).orEmpty(),
                summary = getStringExtra(EXTRA_SUMMARY).orEmpty(),
                source = getStringExtra(EXTRA_SOURCE).orEmpty(),
                sourceLogoUrl = getStringExtra(EXTRA_SOURCE_LOGO_URL).orEmpty(),
                publishedAt = getLongExtra(EXTRA_PUBLISHED_AT, 0L),
                publishedText = getStringExtra(EXTRA_PUBLISHED_TEXT).orEmpty(),
                sourceCount = getIntExtra(EXTRA_SOURCE_COUNT, 1),
            )
        }
    }
}
