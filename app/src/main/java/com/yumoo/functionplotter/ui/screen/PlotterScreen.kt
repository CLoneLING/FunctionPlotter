package com.yumoo.functionplotter.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yumoo.functionplotter.ui.component.ExpressionInput
import com.yumoo.functionplotter.ui.component.FunctionCanvas
import com.yumoo.functionplotter.ui.component.MathKeyboard
import com.yumoo.functionplotter.viewmodel.PlotterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlotterScreen(
    viewModel: PlotterViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToInfo: () -> Unit
) {
    val expression by viewModel.expression.collectAsState()
    val plotted by viewModel.plotted.collectAsState()
    val showGrid by viewModel.showGrid.collectAsState(initial = true)
    val showAxisLabels by viewModel.showAxisLabels.collectAsState(initial = true)
    val sampleStep by viewModel.sampleStep.collectAsState(initial = 2f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("函数图像") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                    IconButton(onClick = onNavigateToInfo) {
                        Icon(Icons.Default.Info, contentDescription = "信息")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            ExpressionInput(
                value = expression,
                onValueChange = viewModel::onExpressionChange
            )
            FunctionCanvas(
                expression = plotted,
                showGrid = showGrid,
                showAxisLabels = showAxisLabels,
                sampleStep = sampleStep,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            MathKeyboard(
                onKey = viewModel::appendToExpression,
                onBackspace = viewModel::backspace,
                onClear = viewModel::clear,
                onPlot = viewModel::plot
            )
        }
    }
}