package com.yumoo.functionplotter.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yumoo.functionplotter.ui.theme.AtomicShapes
import kotlinx.coroutines.launch

private enum class KeyAction { Insert, Backspace, Clear, Replace, LogBase }

private data class CalcKey(
    val label: String,
    val insert: String = label,
    val hint: String? = null,
    val action: KeyAction = KeyAction.Insert,
    val accent: Boolean = false,
    val danger: Boolean = false
)

private data class CalcPage(val title: String, val rows: List<List<CalcKey>>)

private fun key(
    label: String,
    insert: String = label,
    hint: String? = null,
    accent: Boolean = false,
    danger: Boolean = false,
    action: KeyAction = KeyAction.Insert
) = CalcKey(label, insert, hint, action, accent, danger)

private fun example(label: String, expression: String) =
    CalcKey(label = label, insert = expression, action = KeyAction.Replace, accent = true)

private val PAGES: List<CalcPage> = listOf(
    CalcPage(
        title = "基础",
        rows = listOf(
            listOf(
                key("7"), key("8"), key("9"), key("("), key(")"),
                key("⌫", action = KeyAction.Backspace)
            ),
            listOf(
                key("4"), key("5"), key("6"),
                key("×", "*"), key("÷", "/"),
                key("清空", action = KeyAction.Clear, danger = true)
            ),
            listOf(
                key("1"), key("2"), key("3"),
                key("−", "-"), key("+"), key("=", hint = "方程")
            ),
            listOf(
                key("0"), key("."),
                key("x", hint = "自变量"),
                key("y", hint = "因变量"),
                key("π", "π", hint = "圆周率"),
                key("e", "e", hint = "自然常数")
            )
        )
    ),
    CalcPage(
        title = "函数",
        rows = listOf(
            listOf(
                key("sin", "sin(", "正弦", accent = true),
                key("cos", "cos(", "余弦", accent = true),
                key("tan", "tan(", "正切", accent = true),
                key("ln", "ln(", "自然对数", accent = true),
                key("lg", "lg(", "常用对数", accent = true),
                key("logₐ", "log", "任意底", accent = true, action = KeyAction.LogBase)
            ),
            listOf(
                key("arcsin", "arcsin(", "反正弦"),
                key("arccos", "arccos(", "反余弦"),
                key("arctan", "arctan(", "反正切"),
                key("√", "√(", "平方根"),
                key("∛", "∛(", "立方根"),
                key("1/x", "1/(", "倒数")
            ),
            listOf(
                key("sinh", "sinh(", "双曲正弦"),
                key("cosh", "cosh(", "双曲余弦"),
                key("tanh", "tanh(", "双曲正切"),
                key("x²", "^2", "平方"),
                key("x³", "^3", "立方"),
                key("abs(", "abs(", "绝对值")
            ),
            listOf(
                key("eˣ", "e^", "指数"),
                key("10ˣ", "10^", "十的幂"),
                key("2ˣ", "2^", "二的幂"),
                key("floor(", "floor(", "向下取整"),
                key("ceil(", "ceil(", "向上取整"),
                key("|", "|", "绝对值竖线")
            )
        )
    ),
    CalcPage(
        title = "高级",
        rows = listOf(
            listOf(
                key("pow(", "pow(", "任意次幂"),
                key("cbrt(", "cbrt(", "立方根"),
                key("log(", "log(", "自然对数"),
                key("signum(", "signum(", "符号函数"),
                key("%", "%", "取余"),
                key(",", ",", "分隔参数")
            ),
            listOf(
                key("(", "("), key(")", ")"),
                key("^", "^", "幂"),
                key("x^", "x^", "x 的幂"),
                key("√", "√(", "平方根"),
                key("⌫", action = KeyAction.Backspace)
            ),
            listOf(
                key("floor(", "floor(", "向下取整"),
                key("ceil(", "ceil(", "向上取整"),
                key("abs(", "abs(", "绝对值"),
                key("exp(", "exp(", "e 的幂"),
                key("sqrt(", "sqrt(", "平方根"),
                key("1/(", "1/(", "倒数")
            )
        )
    ),
    CalcPage(
        title = "常数",
        rows = listOf(
            listOf(
                key("π", "π", "圆周率"),
                key("e", "e", "自然常数"),
                key("x"), key("y"),
                key("=", hint = "方程"),
                key(",", ",", "分隔参数")
            ),
            listOf(
                key("⌫", action = KeyAction.Backspace),
                key("清空", action = KeyAction.Clear, danger = true),
                key("0"), key("."), key("("), key(")")
            ),
            listOf(
                example("抛物线", "x^2"),
                example("正弦", "sin(x)"),
                example("双曲", "x^2-y^2=1"),
                example("圆", "x^2+y^2=9"),
                example("心形", "(x^2+y^2-1)^3-x^2*y^3=0"),
                example("指数", "e^x")
            )
        )
    )
)

/**
 * 分页数学键盘：左右滑动或点页签切换。
 * 每个按键都带小字说明，照顾不熟悉 `sqrt(`、`ln(` 这类写法的用户；
 * 插入的是"人类写法"（√、ln、logₐ），由 ExpressionNormalizer 翻译成 exp4j 语法。
 */
@Composable
fun MathKeyboard(
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onReplace: (String) -> Unit,
    showHints: Boolean = true,
    showTabs: Boolean = true,
    keyHeight: androidx.compose.ui.unit.Dp = 44.dp
) {
    val pagerState = rememberPagerState { PAGES.size }
    val scope = rememberCoroutineScope()
    var showBaseDialog by remember { mutableStateOf(false) }

    val maxRows = remember { PAGES.maxOf { it.rows.size } }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        // 页签（可在设置中隐藏，隐藏后左右滑动或点页码仍可切换）
        if (showTabs) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PAGES.forEachIndexed { index, page ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
                            )
                            .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = page.title,
                        fontSize = 12.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height((keyHeight + 6.dp) * maxRows + 4.dp),
            verticalAlignment = Alignment.Top
        ) { pageIndex ->
            val page = PAGES[pageIndex]
            Column(Modifier.fillMaxWidth()) {
                page.rows.forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { calcKey ->
                            KeyButton(
                                calcKey = calcKey,
                                showHints = showHints,
                                keyHeight = keyHeight,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    when (calcKey.action) {
                                        KeyAction.Insert -> if (calcKey.insert.isNotEmpty()) onKey(calcKey.insert)
                                        KeyAction.Backspace -> onBackspace()
                                        KeyAction.Clear -> onClear()
                                        KeyAction.Replace -> onReplace(calcKey.insert)
                                        KeyAction.LogBase -> showBaseDialog = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // 页码指示
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(PAGES.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    Modifier
                        .padding(horizontal = 2.dp)
                        .size(if (selected) 6.dp else 4.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                )
            }
        }
    }

    if (showBaseDialog) {
        LogBaseDialog(
            onDismiss = { showBaseDialog = false },
            onPick = { insertion ->
                showBaseDialog = false
                onKey(insertion)
            }
        )
    }
}

@Composable
private fun KeyButton(
    calcKey: CalcKey,
    modifier: Modifier,
    showHints: Boolean,
    keyHeight: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val containerColor = when {
        calcKey.danger -> MaterialTheme.colorScheme.errorContainer
        calcKey.accent -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    }
    val contentColor = when {
        calcKey.danger -> MaterialTheme.colorScheme.onErrorContainer
        calcKey.accent -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.955f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "keyScale"
    )

    FilledTonalButton(
        onClick = onClick,
        shape = AtomicShapes.Key,
        contentPadding = PaddingValues(0.dp),
        interactionSource = interaction,
        modifier = modifier
            .padding(3.dp)
            .height(keyHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = calcKey.label,
                fontSize = if (calcKey.label.length >= 4) 9.sp
                else if (calcKey.label.length >= 3) 11.sp
                else 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            if (showHints && calcKey.hint != null) {
                Text(
                    text = calcKey.hint,
                    fontSize = 7.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun LogBaseDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择对数的底数") },
        text = {
            Column(horizontalAlignment = Alignment.Start) {
                Text("对数 logₐ(b) 表示「a 的几次方等于 b」。")
                Text("例如 log₂(8) = 3，因为 2³ = 8。")
                Text(
                    text = "自定义底数：选「自定义」后输入 log(3, 8)，表示以 3 为底的 8 的对数。",
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onPick("log2(") }) { Text("2") }
                TextButton(onClick = { onPick("log10(") }) { Text("10") }
                TextButton(onClick = { onPick("ln(") }) { Text("e") }
                TextButton(onClick = { onPick("log(") }) { Text("自定义") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
