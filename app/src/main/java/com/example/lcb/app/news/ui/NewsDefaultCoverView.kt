package com.example.lcb.app.news.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import com.example.lcb.app.R

class NewsDefaultCoverView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val bounds = RectF()
    private val clipPath = Path()
    private val panelPath = Path()
    private val badgeBounds = RectF()
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(242, 51, 51)
        style = Paint.Style.FILL
    }
    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 255, 255)
        style = Paint.Style.FILL
    }
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(60, 255, 255, 255)
        strokeWidth = dp(1f)
    }
    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 255, 255, 255)
        strokeCap = Paint.Cap.ROUND
    }
    private val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cornerRadius = dp(10f)
    private val badgeText = context.getString(R.string.flash_default_cover_badge)
    private var progress = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1700L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
        doOnEnd {
            progress = 0f
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        syncAnimation()
    }

    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        syncAnimation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        bounds.set(0f, 0f, w.toFloat(), h.toFloat())
        clipPath.reset()
        clipPath.addRoundRect(bounds, cornerRadius, cornerRadius, Path.Direction.CW)
        backgroundPaint.shader = LinearGradient(
            0f,
            0f,
            w.toFloat(),
            h.toFloat(),
            intArrayOf(
                Color.rgb(31, 39, 52),
                Color.rgb(55, 67, 83),
                Color.rgb(20, 25, 35),
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawRect(bounds, backgroundPaint)
        drawNewsCover(canvas)
        drawShine(canvas)
        canvas.restore()
    }

    private fun drawNewsCover(canvas: Canvas) {
        val width = bounds.width()
        val height = bounds.height()

        drawAbstractPhotoPanels(canvas, width, height)
        drawNewsBadge(canvas, width, height)
        drawLowerThird(canvas, width, height)
    }

    private fun drawAbstractPhotoPanels(canvas: Canvas, width: Float, height: Float) {
        panelPaint.color = Color.argb(48, 255, 255, 255)
        panelPath.reset()
        panelPath.moveTo(width * 0.08f, height * 0.18f)
        panelPath.lineTo(width * 0.48f, height * 0.05f)
        panelPath.lineTo(width * 0.34f, height * 0.66f)
        panelPath.lineTo(width * 0.02f, height * 0.72f)
        panelPath.close()
        canvas.drawPath(panelPath, panelPaint)

        panelPaint.color = Color.argb(36, 242, 51, 51)
        panelPath.reset()
        panelPath.moveTo(width * 0.58f, 0f)
        panelPath.lineTo(width, 0f)
        panelPath.lineTo(width, height * 0.55f)
        panelPath.lineTo(width * 0.42f, height * 0.7f)
        panelPath.close()
        canvas.drawPath(panelPath, panelPaint)

        dividerPaint.alpha = 54
        canvas.drawLine(width * 0.18f, height * 0.18f, width * 0.88f, height * 0.56f, dividerPaint)
        canvas.drawLine(width * 0.34f, height * 0.12f, width * 0.18f, height * 0.72f, dividerPaint)
    }

    private fun drawNewsBadge(canvas: Canvas, width: Float, height: Float) {
        val badgeLeft = width * 0.1f
        val badgeTop = height * 0.16f
        val badgeHeight = height * 0.22f
        val badgeWidth = width * 0.34f
        badgeBounds.set(badgeLeft, badgeTop, badgeLeft + badgeWidth, badgeTop + badgeHeight)

        canvas.drawRoundRect(badgeBounds, dp(4f), dp(4f), redPaint)
        badgeTextPaint.textSize = (height * 0.11f).coerceAtLeast(dp(8f))
        val baseline = badgeBounds.centerY() - (badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2f
        canvas.drawText(badgeText, badgeBounds.centerX(), baseline, badgeTextPaint)
    }

    private fun drawLowerThird(canvas: Canvas, width: Float, height: Float) {
        val stripTop = height * 0.64f
        redPaint.alpha = 220
        canvas.drawRect(0f, stripTop, width, height * 0.78f, redPaint)
        redPaint.alpha = 255

        headlinePaint.strokeWidth = dp(3f)
        headlinePaint.alpha = 220
        canvas.drawLine(width * 0.1f, height * 0.86f, width * 0.72f, height * 0.86f, headlinePaint)
        headlinePaint.strokeWidth = dp(2f)
        headlinePaint.alpha = 150
        canvas.drawLine(width * 0.1f, height * 0.93f, width * 0.5f, height * 0.93f, headlinePaint)

        whitePaint.alpha = 34
        canvas.drawRect(0f, height * 0.78f, width, height, whitePaint)
        whitePaint.alpha = 255
    }

    private fun drawShine(canvas: Canvas) {
        val width = bounds.width()
        val height = bounds.height()
        val shineWidth = width * 0.36f
        val startX = -shineWidth + (width + shineWidth * 2f) * progress
        shinePaint.shader = LinearGradient(
            startX,
            0f,
            startX + shineWidth,
            height,
            intArrayOf(Color.TRANSPARENT, Color.argb(80, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(bounds, shinePaint)
        shinePaint.shader = null
    }

    private fun syncAnimation() {
        if (isShown && isAttachedToWindow && ValueAnimator.areAnimatorsEnabled()) {
            startAnimation()
        } else {
            stopAnimation()
        }
    }

    private fun startAnimation() {
        if (!animator.isStarted) {
            animator.start()
        }
    }

    private fun stopAnimation() {
        if (animator.isStarted) {
            animator.cancel()
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
