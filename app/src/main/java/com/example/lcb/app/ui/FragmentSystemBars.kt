package com.example.lcb.app.ui

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.example.lcb.app.R

/**
 * Fragment 级系统栏适配入口。
 *
 * decorFitsSystemWindows 是 Window 级 API，但调用方放在 Fragment 内部，
 * 让每个页面自己决定系统栏颜色、内容安全区和 CoordinatorLayout 的适配方式。
 */
object FragmentSystemBars {
    @Suppress("DEPRECATION")
    fun applyEdgeToEdge(
        fragment: Fragment,
        statusBarColor: Int = Color.TRANSPARENT,
        navigationBarColor: Int = Color.TRANSPARENT,
        lightStatusBars: Boolean = true,
        lightNavigationBars: Boolean = true,
    ) {
        val window = fragment.requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = statusBarColor
        window.navigationBarColor = navigationBarColor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = lightStatusBars
            isAppearanceLightNavigationBars = lightNavigationBars
        }
    }

    fun applyBottomNavigationInsets(activity: Activity, insets: WindowInsetsCompat) {
        val bottomNavigation = activity.findViewById<View>(R.id.bottomNavigation) ?: return
        val initialPadding = bottomNavigation.initialSystemBarPadding()
        bottomNavigation.updatePadding(
            left = initialPadding.left + SystemBarInsets.left(insets),
            right = initialPadding.right + SystemBarInsets.right(insets),
            bottom = initialPadding.bottom + SystemBarInsets.navigationBottom(insets),
        )
    }

    fun applyTopInsetToFixedHeightView(view: View, insets: WindowInsetsCompat) {
        val initialPadding = view.initialSystemBarPadding()
        val initialHeight = view.initialSystemBarHeight()
        val statusTop = SystemBarInsets.top(insets)

        view.updatePadding(top = initialPadding.top + statusTop)
        if (initialHeight > 0) {
            view.updateLayoutParams {
                height = initialHeight + statusTop
            }
        }
    }

    fun requestInsets(view: View) {
        ViewCompat.requestApplyInsets(view)
    }

    private fun View.initialSystemBarHeight(): Int {
        val current = getTag(R.id.tag_system_bar_initial_height) as? Int
        if (current != null) return current

        return layoutParams.height.also { setTag(R.id.tag_system_bar_initial_height, it) }
    }

    private fun View.initialSystemBarPadding(): InitialPadding {
        val current = getTag(R.id.tag_system_bar_initial_padding) as? InitialPadding
        if (current != null) return current

        return InitialPadding(
            left = paddingLeft,
            top = paddingTop,
            right = paddingRight,
            bottom = paddingBottom,
        ).also { setTag(R.id.tag_system_bar_initial_padding, it) }
    }

    private data class InitialPadding(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    )
}
