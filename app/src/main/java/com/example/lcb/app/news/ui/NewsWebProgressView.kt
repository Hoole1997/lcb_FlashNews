package com.example.lcb.app.news.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.lcb.app.R

class NewsWebProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.flash_red)
        strokeCap = Paint.Cap.ROUND
    }
    private var progress = 0

    fun setProgress(value: Int) {
        progress = value.coerceIn(0, 100)
        visibility = if (progress in 1..99) VISIBLE else GONE
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (progress <= 0) return
        val end = width * (progress / 100f)
        paint.strokeWidth = height.toFloat().coerceAtLeast(1f)
        canvas.drawLine(0f, height / 2f, end, height / 2f, paint)
    }
}
