package com.yumoo.functionplotter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun FunctionPlotterTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = OceanBlue,
            secondary = AccentRed,
            background = DarkBackground,
            surface = DarkSurface,
            onBackground = DarkOnBackground,
            onSurface = DarkOnSurface
        )
    } else {
        lightColorScheme(
            primary = OceanBlue,
            secondary = AccentRed,
            background = LightBackground,
            surface = LightSurface,
            onBackground = LightOnBackground,
            onSurface = LightOnSurface
        )
    }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
