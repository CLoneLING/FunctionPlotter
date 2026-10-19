package com.yumoo.functionplotter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.yumoo.functionplotter.ui.navigation.AppNav
import com.yumoo.functionplotter.ui.theme.FunctionPlotterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as FunctionPlotterApp).settingsRepository

        setContent {
            val themeMode by repo.themeMode.collectAsState(initial = 0)
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
}