package com.yumoo.functionplotter.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yumoo.functionplotter.data.model.PlotFunction
import com.yumoo.functionplotter.domain.parser.Exp4jEvaluator
import com.yumoo.functionplotter.domain.parser.ExpressionCheck
import com.yumoo.functionplotter.domain.parser.checkExpression
import com.yumoo.functionplotter.ui.component.CanvasIsland
import com.yumoo.functionplotter.ui.component.ExpressionInput
import com.yumoo.functionplotter.ui.component.FunctionCanvas
import com.yumoo.functionplotter.ui.component.FunctionChips
import com.yumoo.functionplotter.ui.component.MathKeyboard
import com.yumoo.functionplotter.ui.component.SyntaxHelpDialog
import com.yumoo.functionplotter.ui.component.UpdateBanner
import com.yumoo.functionplotter.ui.theme.AtomicShapes
import com.yumoo.functionplotter.ui.theme.atomicSurface
import com.yumoo.functionplotter.ui.util.rememberFoldingFeature
import com.yumoo.functionplotter.ui.util.rememberWindowClass
import androidx.window.layout.FoldingFeature
import com.yumoo.functionplotter.viewmodel.PlotterViewModel
import com.yumoo.functionplotter.viewmodel.UpdateViewModel

private data class CanvasArgs(
    val withIsland: Boolean,
    val resetToken: Int,
    val functions: List<PlotFunction>,
    val showGrid: Boolean,
    val showAxisLabels: Boolean,
    val sampleStep: Float,
    val onInvalid: (Set<Long>) -> Unit
)

@Composable
fun PlotterScreen(
    viewModel: PlotterViewModel,
    updateViewModel: UpdateViewModel,
    onNavigateToSettings: () -> Unit
) {
    val functions by viewModel.functions.collectAsState()
    val selectedId by viewModel.selectedId.collectAsState()
    val draft by viewModel.draft.collectAsState()
    val showGrid by viewModel.showGrid.collectAsState(initial = true)
    val showAxisLabels by viewModel.showAxisLabels.collectAsState(initial = true)
    val sampleStep by viewModel.sampleStep.collectAsState(initial = 2f)
    val showHelpButton by viewModel.showHelpButton.collectAsState(initial = true)
    val showKeyHints by viewModel.showKeyHints.collectAsState(initial = false)
    val showKeyTabs by viewModel.showKeyTabs.collectAsState(initial = false)

    val windowClass = rememberWindowClass()

    val evaluator = remember { Exp4jEvaluator() }
    val draftError = remember(draft) {
        if (draft.isBlank()) {
            null
        } else {
            (checkExpression(draft, evaluator) as? ExpressionCheck.Invalid)?.message
        }
    }

    var invalidIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showHelp by remember { mutableStateOf(false) }
    var resetToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { updateViewModel.autoCheckOnce() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) updateViewModel.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val chipsRow: @Composable () -> Unit = {
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
                invalidIds = invalidIds,
                onSelect = viewModel::selectFunction,
                onRemove = viewModel::removeFunction,
                onAdd = viewModel::addFunction,
                canAdd = functions.size < PlotterViewModel.MAX_FUNCTIONS,
                modifier = Modifier.weight(1f)
            )
            if (showHelpButton) {
                CircleTextButton(text = "?", onClick = { showHelp = true })
            }
            CircleIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "设置",
                onClick = onNavigateToSettings
            )
        }
    }

    val inputArea: @Composable () -> Unit = {
        ExpressionInput(
            value = draft,
            error = draftError,
            onValueChange = viewModel::onDraftChange,
            onPlot = viewModel::applyDraft
        )
    }

    val keyboard: @Composable (Dp) -> Unit = { keyHeight ->
        MathKeyboard(
            onKey = viewModel::appendToDraft,
            onBackspace = viewModel::backspace,
            onClear = viewModel::clearDraft,
            onReplace = viewModel::replaceDraft,
            showHints = showKeyHints,
            showTabs = showKeyTabs,
            keyHeight = keyHeight
        )
    }

    // movableContentOf：横竖屏布局切换时画布"搬家"而不是重建，
    // 内部的相机视角、读数标记等状态得以保留。
    val canvasContent = remember {
        movableContentOf<CanvasArgs> { args ->
            FunctionCanvas(
                functions = args.functions,
                showGrid = args.showGrid,
                showAxisLabels = args.showAxisLabels,
                sampleStep = args.sampleStep,
                modifier = Modifier.fillMaxSize(),
                resetToken = args.resetToken,
                rootsAtBottomEnd = args.withIsland,
                onInvalidIdsChange = args.onInvalid
            )
        }
    }

    val canvasPane: @Composable (Boolean) -> Unit = { withIsland ->
        Box(Modifier.fillMaxSize()) {
            canvasContent(
                CanvasArgs(
                    withIsland = withIsland,
                    resetToken = resetToken,
                    functions = functions,
                    showGrid = showGrid,
                    showAxisLabels = showAxisLabels,
                    sampleStep = sampleStep,
                    onInvalid = { invalidIds = it }
                )
            )
            if (withIsland) {
                CanvasIsland(
                    onResetView = { resetToken++ },
                    onClearInput = viewModel::clearDraft,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                )
            }
        }
    }

    val container = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .systemBarsPadding()
        .padding(horizontal = 8.dp)

    if (windowClass.useTwoPane) {
        val density = LocalDensity.current
        val folding = rememberFoldingFeature()
        val verticalHinge = folding != null &&
            folding.isSeparating &&
            folding.orientation == FoldingFeature.Orientation.VERTICAL
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp

        // 折叠屏：左栏铺到铰链左边缘，铰链宽度整段留空；平板：左栏占 34%
        val controlsWidth = if (verticalHinge && folding != null) {
            with(density) { folding.bounds.left.toDp() } - 8.dp
        } else {
            (screenWidth * 0.34f).coerceIn(320.dp, 520.dp)
        }.coerceIn(260.dp, screenWidth - 240.dp)
        val hingeGap = if (verticalHinge && folding != null) {
            with(density) { folding.bounds.width().toDp() }
        } else {
            8.dp
        }

        Row(modifier = container) {
            Column(
                modifier = Modifier
                    .width(controlsWidth)
                    .fillMaxHeight()
            ) {
                chipsRow()
                inputArea()
                UpdateBanner(viewModel = updateViewModel)
                Spacer(Modifier.weight(1f))
                LargeScreenInfo(
                    functions = functions,
                    selectedId = selectedId,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                Spacer(Modifier.weight(1f))
                keyboard(52.dp)
            }

            Spacer(Modifier.width(hingeGap))

            // 画布做成圆角面板，弱化左右分区感
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(AtomicShapes.Card)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = AtomicShapes.Card
                    )
            ) {
                canvasPane(true)
            }
        }
    } else {
        Column(modifier = container) {
            chipsRow()
            inputArea()
            UpdateBanner(viewModel = updateViewModel)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                canvasPane(false)
            }

            keyboard(44.dp)
        }
    }

    if (showHelp) {
        SyntaxHelpDialog(onDismiss = { showHelp = false })
    }
}

/** 大屏横屏专属：填充左栏中部，展示当前函数与操作提示。 */
@Composable
private fun LargeScreenInfo(
    functions: List<com.yumoo.functionplotter.data.model.PlotFunction>,
    selectedId: Long,
    modifier: Modifier = Modifier
) {
    val selected = functions.firstOrNull { it.id == selectedId }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .atomicSurface(com.yumoo.functionplotter.ui.theme.AtomicShapes.Card)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "当前函数",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
        Text(
            text = selected?.expression?.ifBlank { "(空)" } ?: "—",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2
        )
        Text(
            text = "共 ${functions.size} 条 · 点上方标签切换",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.padding(top = 2.dp))
        Text(
            text = "拖动平移 · 双指缩放 · 点按读数",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
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

@Composable
private fun CircleTextButton(
    text: String,
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
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            modifier = Modifier.padding(bottom = 1.dp)
        )
    }
}