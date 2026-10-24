package com.yumoo.functionplotter.ui.util

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo

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

/**
 * 当前折叠状态。真折叠屏返回 [FoldingFeature]；平板返回 null。
 * 用 WindowManager 官方接口，避免按分辨率猜铰链位置。
 */
@Composable
fun rememberFoldingFeature(): FoldingFeature? {
    val context = LocalContext.current
    val activity = context as? Activity ?: return null
    val tracker = WindowInfoTracker.getOrCreate(context)
    val info: WindowLayoutInfo by tracker.windowLayoutInfo(activity)
        .collectAsState(initial = WindowLayoutInfo(emptyList()))
    return info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
}

/** 平板/折叠屏展开态的判定阈值。 */
const val LARGE_SCREEN_WIDTH_DP = 600