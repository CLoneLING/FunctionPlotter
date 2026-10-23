package com.yumoo.functionplotter.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AtomicShapesSpec = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun FunctionPlotterTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = OceanBlue,
            onPrimary = DarkOnBackground,
            primaryContainer = OceanBlue.copy(alpha = 0.22f),
            onPrimaryContainer = OceanBlueSoft,
            secondary = AccentRed,
            background = DarkBackground,
            onBackground = DarkOnBackground,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurface.copy(alpha = 0.6f),
            onSurfaceVariant = DarkOnSurface.copy(alpha = 0.8f),
            outlineVariant = DarkOnSurface.copy(alpha = 0.14f),
            errorContainer = AccentRed.copy(alpha = 0.18f)
        )
    } else {
        lightColorScheme(
            primary = OceanBlue,
            onPrimary = LightSurface,
            primaryContainer = OceanBlue.copy(alpha = 0.12f),
            onPrimaryContainer = OceanBlue,
            secondary = AccentRed,
            background = LightBackground,
            onBackground = LightOnBackground,
            surface = LightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightBackground,
            onSurfaceVariant = LightOnSurface.copy(alpha = 0.75f),
            outlineVariant = LightOnBackground.copy(alpha = 0.10f),
            errorContainer = AccentRed.copy(alpha = 0.12f)
        )
    }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AtomicShapesSpec,
        content = content
    )
}
