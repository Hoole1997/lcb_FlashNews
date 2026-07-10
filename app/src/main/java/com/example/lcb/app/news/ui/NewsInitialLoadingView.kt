package com.example.lcb.app.news.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.example.lcb.app.R
import kotlin.math.PI
import kotlin.math.sin

/**
 * 纯版式新闻骨架。错峰呼吸模拟编辑台逐栏排版，窄高光负责提示加载进度；
 * 所有动画都在单个 Canvas 内完成，不创建临时对象或触发布局重算。
 */
class NewsInitialLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val density = resources.displayMetrics.density
    private val skeletonColor = ContextCompat.getColor(context, R.color.flash_divider)
    private val skeletonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = skeletonColor }
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.flash_chip_stroke)
        alpha = 120
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.flash_red)
    }
    private val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sheenMatrix = Matrix()
    private val storyPaths = ArrayList<Path>(5)
    private var sheenWidth = 0f
    private var phase = 0f
    private var animator: ValueAnimator? = null

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        buildStoryPaths(width, height)
        sheenWidth = width * 0.22f
        sheenPaint.shader = LinearGradient(
            -sheenWidth,
            0f,
            sheenWidth,
            0f,
            intArrayOf(0x00FFFFFF, 0x78FFFFFF, 0x00FFFFFF),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = dp(18f)
        val accentPulse = ((sin(phase * 2f * PI) + 1f) / 2f).toFloat()
        canvas.drawRoundRect(
            left,
            dp(18f),
            left + dp(24f + accentPulse * 20f),
            dp(21f),
            dp(1.5f),
            dp(1.5f),
            accentPaint,
        )

        storyPaths.forEachIndexed { index, path ->
            val rowPhase = ((phase - index * 0.11f) % 1f + 1f) % 1f
            val pulse = ((sin(rowPhase * 2f * PI) + 1f) / 2f).toFloat()
            skeletonPaint.alpha = (125 + pulse * 55).toInt()
            canvas.drawPath(path, skeletonPaint)

            val sweepX = rowPhase * (width + sheenWidth * 2f) - sheenWidth
            sheenMatrix.setTranslate(sweepX, 0f)
            sheenPaint.shader?.setLocalMatrix(sheenMatrix)
            canvas.drawPath(path, sheenPaint)

            if (index < storyPaths.lastIndex) {
                val dividerY = dp(118f + index * STORY_STEP_DP)
                canvas.drawRect(left, dividerY, width - left, dividerY + dp(1f), dividerPaint)
            }
        }
    }

    private fun buildStoryPaths(width: Int, height: Int) {
        storyPaths.clear()
        if (width <= 0 || height <= 0) return

        val left = dp(18f)
        val right = width - dp(18f)
        val imageWidth = minOf(dp(108f), width * 0.31f)
        val imageLeft = right - imageWidth
        val textRight = imageLeft - dp(15f)
        val availableHeight = height - dp(38f)
        val rowCount = (availableHeight / dp(STORY_STEP_DP)).toInt().coerceIn(3, 5)

        repeat(rowCount) { index ->
            val top = dp(38f + index * STORY_STEP_DP)
            val path = Path()
            path.addCircle(left + dp(8f), top + dp(8f), dp(8f), Path.Direction.CW)
            path.addRoundRect(
                left + dp(22f),
                top + dp(4f),
                left + dp(76f + index % 2 * 16f),
                top + dp(11f),
                dp(3f),
                dp(3f),
                Path.Direction.CW,
            )
            path.addRoundRect(left, top + dp(24f), textRight, top + dp(37f), dp(3f), dp(3f), Path.Direction.CW)
            path.addRoundRect(
                left,
                top + dp(45f),
                left + (textRight - left) * if (index % 2 == 0) 0.78f else 0.9f,
                top + dp(57f),
                dp(3f),
                dp(3f),
                Path.Direction.CW,
            )
            path.addRoundRect(left, top + dp(68f), left + dp(74f), top + dp(75f), dp(3f), dp(3f), Path.Direction.CW)
            path.addRoundRect(imageLeft, top, right, top + dp(80f), dp(8f), dp(8f), Path.Direction.CW)
            storyPaths += path
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAnimation()
    }

    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (isAttachedToWindow) updateAnimation()
    }

    private fun updateAnimation() {
        if (visibility == VISIBLE && ValueAnimator.areAnimatorsEnabled()) startAnimation() else stopAnimation()
    }

    private fun startAnimation() {
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1_450L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopAnimation() {
        animator?.cancel()
        animator = null
        phase = 0f
        invalidate()
    }

    private fun dp(value: Float): Float = value * density

    private companion object {
        const val STORY_STEP_DP = 99f
    }
}
