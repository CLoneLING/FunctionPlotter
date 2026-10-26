package com.yumoo.functionplotter.ui.util

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

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
 *
 * 注意：这里必须"绝对不起火"。部分非标准 Android 运行环境
 * （如鸿蒙通过卓易通运行、深度定制的 ROM、部分模拟器）没有实现
 * androidx.window 依赖的窗口扩展服务，调 [WindowInfoTracker] 会抛
 * 异常甚至 Error（NoClassDefFoundError）。这个函数在首帧组合时就会执行，
 * 一旦抛出没人接就是启动即闪退，所以整体 try/catch，失败即降级成非折叠屏。
 */
@Composable
fun rememberFoldingFeature(): FoldingFeature? {
    val context = LocalContext.current
    val activity = context as? Activity ?: return null
    var feature by remember { mutableStateOf<FoldingFeature?>(null) }

    LaunchedEffect(activity) {
        try {
            WindowInfoTracker.getOrCreate(context)
                .windowLayoutInfo(activity)
                .collect { info ->
                    feature = try {
                        info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
                    } catch (t: Throwable) {
                        null
                    }
                }
        } catch (t: Throwable) {
            // 服务缺失：当作普通大屏处理，不做铰链分栏
            feature = null
        }
    }
    return feature
}

/** 平板/折叠屏展开态的判定阈值。 */
const val LARGE_SCREEN_WIDTH_DP = 600