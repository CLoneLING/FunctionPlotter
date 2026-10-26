package com.yumoo.functionplotter

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.yumoo.functionplotter.ui.navigation.AppNav
import com.yumoo.functionplotter.ui.theme.FunctionPlotterTheme
import com.yumoo.functionplotter.ui.theme.LocalAnimationsEnabled
import com.yumoo.functionplotter.ui.util.LARGE_SCREEN_WIDTH_DP
import java.time.LocalTime

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 非标准运行环境（鸿蒙卓易通、部分定制 ROM）对 edge-to-edge、
        // 强制方向支持不完整，失败就退回普通窗口，不能让启动流程中断
        runCatching { enableEdgeToEdge() }
        runCatching { applyOrientationPolicy(resources.configuration) }

        val repository = (application as FunctionPlotterApp).settingsRepository

        try {
            setContent {
                val themeMode by repository.themeMode.collectAsState(initial = 0)
                val darkTheme = when (themeMode) {
                    1 -> false
                    2 -> true
                    else -> isSystemInDarkTheme()
                }
                val animations by repository.animations.collectAsState(initial = true)
                val eyeCareMode by repository.eyeCareMode.collectAsState(initial = 1)
                val startHour by repository.eyeCareStart.collectAsState(initial = 20)
                val endHour by repository.eyeCareEnd.collectAsState(initial = 7)
                val warm = runCatching {
                    !darkTheme && eyeCareActive(eyeCareMode, LocalTime.now(), startHour, endHour)
                }.getOrDefault(false)

                CompositionLocalProvider(LocalAnimationsEnabled provides animations) {
                    FunctionPlotterTheme(darkTheme = darkTheme, warm = warm) {
                        AppNav()
                    }
                }
            }
        } catch (t: Throwable) {
            // 首屏组合就失败：记日志 + 给一个能读懂的提示，而不是闪退
            runCatching { CrashLog.write(t) }
            showStartupFailure(t)
        }
    }

    private fun showStartupFailure(error: Throwable) {
        runCatching {
            val view = TextView(this).apply {
                text = "当前运行环境启动失败\n" +
                    "日志：Android/data/com.yumoo.functionplotter/files/crash-*.txt\n\n" +
                    (error.message ?: error.javaClass.simpleName)
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(48, 160, 48, 48)
            }
            setContentView(view)
        }
    }

    /** 折叠/展开、旋转都会回到这里，重新决定是否锁竖屏。 */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        runCatching { applyOrientationPolicy(newConfig) }
    }

    private fun applyOrientationPolicy(configuration: Configuration) {
        val isLarge = configuration.smallestScreenWidthDp >= LARGE_SCREEN_WIDTH_DP
        requestedOrientation = if (isLarge) {
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}

/** 0 关闭 / 1 定时 / 2 常开 */
private fun eyeCareActive(mode: Int, now: LocalTime, startHour: Int, endHour: Int): Boolean = when (mode) {
    2 -> true
    1 -> if (startHour <= endHour) {
        now.hour in startHour until endHour
    } else {
        now.hour >= startHour || now.hour < endHour
    }
    else -> false
}