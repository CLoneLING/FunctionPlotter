package com.yumoo.functionplotter.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yumoo.functionplotter.ui.component.UpdateHost
import com.yumoo.functionplotter.ui.screen.InfoScreen
import com.yumoo.functionplotter.ui.screen.PlotterScreen
import com.yumoo.functionplotter.ui.screen.SettingsScreen
import com.yumoo.functionplotter.viewmodel.PlotterViewModel
import com.yumoo.functionplotter.viewmodel.SettingsViewModel
import com.yumoo.functionplotter.viewmodel.UpdateViewModel

object Routes {
    const val PLOTTER = "plotter"
    const val SETTINGS = "settings"
    const val INFO = "info"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    // 作用域挂在 Activity 上：自动检查/手动检查/下载共用同一份状态
    val updateViewModel: UpdateViewModel = viewModel()

    Box(Modifier.fillMaxSize()) {
        NavHost(navController = nav, startDestination = Routes.PLOTTER) {
            composable(Routes.PLOTTER) {
                val vm: PlotterViewModel = viewModel()
                PlotterScreen(
                    viewModel = vm,
                    updateViewModel = updateViewModel,
                    onNavigateToSettings = { nav.navigate(Routes.SETTINGS) },
                    onNavigateToInfo = { nav.navigate(Routes.INFO) }
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
            composable(Routes.INFO) {
                InfoScreen(onBack = { nav.popBackStack() })
            }
        }

        UpdateHost(updateViewModel)
    }
}
