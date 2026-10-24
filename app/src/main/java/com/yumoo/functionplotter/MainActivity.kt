package com.yumoo.functionplotter

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.yumoo.functionplotter.ui.navigation.AppNav
import com.yumoo.functionplotter.ui.theme.FunctionPlotterTheme
import com.yumoo.functionplotter.ui.util.LARGE_SCREEN_WIDTH_DP

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
            FunctionPlotterTheme(darkTheme = darkTheme) {
                AppNav()
            }
        }
    }

    /** 折叠/展开、旋转都会回到这里，重新决定是否锁竖屏。 */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyOrientationPolicy(newConfig)
    }

    /**
     * 手机锁竖屏（小屏横屏太局促）；大屏允许自由旋转。
     * 折叠屏合盖后 sw 变小会自动落回竖屏，展开后恢复可旋转。
     */
    private fun applyOrientationPolicy(configuration: Configuration) {
        val isLarge = configuration.smallestScreenWidthDp >= LARGE_SCREEN_WIDTH_DP
        requestedOrientation = if (isLarge) {
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}