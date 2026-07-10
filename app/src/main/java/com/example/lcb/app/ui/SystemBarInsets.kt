package com.example.lcb.app.ui

import androidx.core.view.WindowInsetsCompat

/**
 * 系统栏安全区统一计算入口。
 *
 * 部分全面屏/挖孔屏设备在 edge-to-edge 模式下，statusBars() 与 displayCutout()
 * 的可见 Insets 可能只返回其中之一；这里统一取可见值和稳定值的最大值。
 */
object SystemBarInsets {
    fun top(insets: WindowInsetsCompat): Int {
        val visible = insets.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout())
        val stable = insets.getInsetsIgnoringVisibility(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout(),
        )
        return maxOf(visible.top, stable.top)
    }

    fun left(insets: WindowInsetsCompat): Int {
        val visible = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val stable = insets.getInsetsIgnoringVisibility(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
        )
        return maxOf(visible.left, stable.left)
    }

    fun right(insets: WindowInsetsCompat): Int {
        val visible = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val stable = insets.getInsetsIgnoringVisibility(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
        )
        return maxOf(visible.right, stable.right)
    }

    fun navigationBottom(insets: WindowInsetsCompat): Int {
        val navigationTypes = WindowInsetsCompat.Type.navigationBars() or
            WindowInsetsCompat.Type.mandatorySystemGestures() or
            WindowInsetsCompat.Type.tappableElement()
        val visible = insets.getInsets(navigationTypes)
        val stable = insets.getInsetsIgnoringVisibility(navigationTypes)
        return maxOf(visible.bottom, stable.bottom)
    }
}
