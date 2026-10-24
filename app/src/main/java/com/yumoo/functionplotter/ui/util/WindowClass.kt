package com.yumoo.functionplotter.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * 设备窗口形态。
 *
 * 用 smallestScreenWidthDp 判定大屏：这是 Android 官方断点（600dp），
 * 不受宽高比影响，折叠屏展开后通常自动落入大屏。
 */
data class WindowClass(
    val isLarge: Boolean,
    val isLandscape: Boolean
) {
    /** 大屏横屏才启用双栏布局。 */
    val useTwoPane: Boolean get() = isLarge && isLandscape
}

@Composable
@ReadOnlyComposable
fun rememberWindowClass(): WindowClass {
    val configuration = LocalConfiguration.current
    return WindowClass(
        isLarge = configuration.smallestScreenWidthDp >= LARGE_SCREEN_WIDTH_DP,
        isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    )
}

/** 平板/折叠屏展开态的判定阈值。 */
const val LARGE_SCREEN_WIDTH_DP = 600
