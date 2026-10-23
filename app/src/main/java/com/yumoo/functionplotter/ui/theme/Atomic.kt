package com.yumoo.functionplotter.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/** 统一的圆角尺度，全应用只从这里取。 */
object AtomicShapes {
    val Card = RoundedCornerShape(28.dp)
    val Control = RoundedCornerShape(20.dp)
    val Key = RoundedCornerShape(18.dp)
    val Chip = RoundedCornerShape(16.dp)
}

/**
 * 原子风格的"通透"表面：自上而下的极淡渐变 + 1px 高光描边 + 柔和投影。
 *
 * 浅色模式靠投影建立层次，深色模式改用顶部高光描边（深色下投影几乎不可见）。
 */
@Composable
fun Modifier.atomicSurface(
    shape: Shape = AtomicShapes.Card,
    elevated: Boolean = true,
    tint: Color = Color.Transparent
): Modifier {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.4f

    val top = if (dark) Color.White.copy(alpha = 0.08f) else Color.White
    val bottom = if (dark) Color.White.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.92f)
    val borderTop = if (dark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.95f)
    val borderBottom = if (dark) Color.White.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.30f)

    var modifier = this
    if (elevated && !dark) {
        modifier = modifier.shadow(
            elevation = 12.dp,
            shape = shape,
            ambientColor = Color(0x14203A63),
            spotColor = Color(0x1F203A63)
        )
    }
    modifier = modifier.clip(shape)

    return if (tint == Color.Transparent) {
        modifier
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .border(1.dp, Brush.verticalGradient(listOf(borderTop, borderBottom)), shape)
    } else {
        modifier
            .background(Brush.verticalGradient(listOf(tint.copy(alpha = 1f), tint.copy(alpha = 0.82f))))
            .border(1.dp, Brush.verticalGradient(listOf(borderTop, borderBottom)), shape)
    }
}
