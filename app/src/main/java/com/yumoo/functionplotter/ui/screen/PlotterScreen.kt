package com.yumoo.functionplotter.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.yumoo.functionplotter.ui.component.ExpressionInput
import com.yumoo.functionplotter.ui.component.FunctionCanvas
import com.yumoo.functionplotter.ui.component.FunctionChips
import com.yumoo.functionplotter.ui.component.MathKeyboard
import com.yumoo.functionplotter.viewmodel.PlotterViewModel

@Composable
fun PlotterScreen(
    viewModel: PlotterViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToInfo: () -> Unit
) {
    val functions by viewModel.functions.collectAsState()
    val selectedId by viewModel.selectedId.collectAsState()
    val draft by viewModel.draft.collectAsState()
    val showGrid by viewModel.showGrid.collectAsState(initial = true)
    val showAxisLabels by viewModel.showAxisLabels.collectAsState(initial = true)
    val sampleStep by viewModel.sampleStep.collectAsState(initial = 2f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 8.dp)
    ) {
        // 顶部一行：函数 chip 栏 + 设置/信息小圆钮（并排，不重叠）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FunctionChips(
                functions = functions,
                selectedId = selectedId,
                onSelect = viewModel::selectFunction,
                onRemove = viewModel::removeFunction,
                onAdd = viewModel::addFunction,
                canAdd = functions.size < PlotterViewModel.MAX_FUNCTIONS,
                modifier = Modifier.weight(1f)
            )
            CircleIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "设置",
                onClick = onNavigateToSettings
            )
            CircleIconButton(
                icon = Icons.Default.Info,
                contentDescription = "信息",
                onClick = onNavigateToInfo
            )
        }

        ExpressionInput(
            value = draft,
            onValueChange = viewModel::onDraftChange,
            onPlot = viewModel::applyDraft
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            FunctionCanvas(
                functions = functions,
                showGrid = showGrid,
                showAxisLabels = showAxisLabels,
                sampleStep = sampleStep,
                modifier = Modifier.fillMaxSize()
            )
        }

        MathKeyboard(
            onKey = viewModel::appendToDraft,
            onBackspace = viewModel::backspace,
            onClear = viewModel::clearDraft
        )
    }
}

@Composable
private fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}