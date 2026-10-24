package com.yumoo.functionplotter.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.yumoo.functionplotter.ui.theme.LocalAnimationsEnabled
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yumoo.functionplotter.ui.component.UpdateHost
import com.yumoo.functionplotter.ui.screen.PlotterScreen
import com.yumoo.functionplotter.ui.screen.SettingsScreen
import com.yumoo.functionplotter.viewmodel.PlotterViewModel
import com.yumoo.functionplotter.viewmodel.SettingsViewModel
import com.yumoo.functionplotter.viewmodel.UpdateViewModel

object Routes {
    const val PLOTTER = "plotter"
    const val SETTINGS = "settings"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    // 作用域挂在 Activity 上：自动检查/手动检查/下载共用同一份状态
    val updateViewModel: UpdateViewModel = viewModel()
    val animations = LocalAnimationsEnabled.current

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = Routes.PLOTTER,
            enterTransition = {
                if (animations) fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.96f)
                else EnterTransition.None
            },
            exitTransition = {
                if (animations) fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.98f)
                else ExitTransition.None
            },
            popEnterTransition = {
                if (animations) fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 1.02f)
                else EnterTransition.None
            },
            popExitTransition = {
                if (animations) fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.98f)
                else ExitTransition.None
            }
        ) {
            composable(Routes.PLOTTER) {
                val vm: PlotterViewModel = viewModel()
                PlotterScreen(
                    viewModel = vm,
                    updateViewModel = updateViewModel,
                    onNavigateToSettings = { nav.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel()
                SettingsScreen(
                    viewModel = vm,
                    updateViewModel = updateViewModel,
                    onBack = { nav.popBackStack() }
                )
            }
        }

        UpdateHost(updateViewModel)
    }
}
