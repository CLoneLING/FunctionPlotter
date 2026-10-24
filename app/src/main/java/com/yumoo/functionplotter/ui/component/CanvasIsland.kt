package com.yumoo.functionplotter.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.yumoo.functionplotter.ui.theme.AtomicShapes
import com.yumoo.functionplotter.ui.theme.atomicSurface

/**
 * 大屏横屏专属的悬浮功能岛：浮在图像区右上角，不占用固定空间。
 * 目前提供两个高频操作——复位视图、清空输入。
 */
@Composable
fun CanvasIsland(
    onResetView: () -> Unit,
    onClearInput: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .atomicSurface(AtomicShapes.Control)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IslandAction(Icons.Default.CenterFocusStrong, "复位视图", onResetView)
        IslandAction(Icons.Default.Backspace, "清空输入", onClearInput)
    }
}

@Composable
private fun IslandAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}
