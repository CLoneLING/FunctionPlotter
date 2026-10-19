package com.yumoo.functionplotter.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yumoo.functionplotter.ui.screen.InfoScreen
import com.yumoo.functionplotter.ui.screen.PlotterScreen
import com.yumoo.functionplotter.ui.screen.SettingsScreen
import com.yumoo.functionplotter.viewmodel.PlotterViewModel
import com.yumoo.functionplotter.viewmodel.SettingsViewModel

object Routes {
    const val PLOTTER = "plotter"
    const val SETTINGS = "settings"
    const val INFO = "info"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.PLOTTER) {
        composable(Routes.PLOTTER) {
            val vm: PlotterViewModel = viewModel()
            PlotterScreen(
                viewModel = vm,
                onNavigateToSettings = { nav.navigate(Routes.SETTINGS) },
                onNavigateToInfo = { nav.navigate(Routes.INFO) }
            )
        }
        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel()
            SettingsScreen(viewModel = vm, onBack = { nav.popBackStack() })
        }
        composable(Routes.INFO) {
            InfoScreen(onBack = { nav.popBackStack() })
        }
    }
}