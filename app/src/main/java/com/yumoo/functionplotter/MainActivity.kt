package com.yumoo.functionplotter

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.yumoo.functionplotter.ui.navigation.AppNav
import com.yumoo.functionplotter.ui.theme.FunctionPlotterTheme
import com.yumoo.functionplotter.ui.theme.LocalAnimationsEnabled
import com.yumoo.functionplotter.ui.util.LARGE_SCREEN_WIDTH_DP
import java.time.LocalTime

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyOrientationPolicy(resources.configuration)

        val repository = (application as FunctionPlotterApp).settingsRepository

        setContent {
            val themeMode by repository.themeMode.collectAsState(initial = 0)
            val darkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            val animations by repository.animations.collectAsState(initial = true)
            val eyeCareMode by repository.eyeCareMode.collectAsState(initial = 1)
            val warm = !darkTheme && eyeCareActive(eyeCareMode, LocalTime.now())

            CompositionLocalProvider(LocalAnimationsEnabled provides animations) {
                FunctionPlotterTheme(darkTheme = darkTheme, warm = warm) {
                    AppNav()
                }
            }
        }
    }

    /** 折叠/展开、旋转都会回到这里，重新决定是否锁竖屏。 */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyOrientationPolicy(newConfig)
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

/** 护眼模式的暖色覆盖层。 */
private val EyeCareTint = Color(0x16FF8A2B)

/** 0 关闭 / 1 定时 20:00–次日 7:00 / 2 常开。 */
private fun eyeCareActive(mode: Int, now: LocalTime): Boolean = when (mode) {
    2 -> true
    1 -> now.hour >= 20 || now.hour < 7
    else -> false
}