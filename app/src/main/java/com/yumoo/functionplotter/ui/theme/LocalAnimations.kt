package com.yumoo.functionplotter.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/** 界面动效开关，由设置项驱动；关闭后所有过渡动画立即完成。 */
val LocalAnimationsEnabled = staticCompositionLocalOf { true }