package com.yumoo.functionplotter.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yumoo.functionplotter.ui.theme.AtomicShapes

/**
 * 表达式输入行：输入框 + 粘贴 + 应用。
 *
 * 输入框不是系统 TextField：键盘由自绘的 [MathKeyboard] 负责，
 * 所以这里自己画文字、自己管光标位置——点一下文字就能把插入点挪过去，
 * 并用一根闪烁的竖线表示"正在输入"。
 */
@Composable
fun ExpressionInput(
    value: String,
    caret: Int,
    error: String?,
    onValueChange: (String) -> Unit,
    onCaretChange: (Int) -> Unit,
    onPlot: () -> Unit
) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ExpressionField(
                value = value,
                caret = caret,
                isError = error != null,
                onCaretChange = onCaretChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
            )

            FilledTonalIconButton(
                onClick = {
                    val text = clipboard.getText()?.text
                    if (!text.isNullOrBlank()) onValueChange(text.trim())
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    Icons.Default.ContentPaste,
                    contentDescription = "粘贴",
                    modifier = Modifier.size(19.dp)
                )
            }

            FilledIconButton(
                onClick = onPlot,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.size(54.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "应用",
                    modifier = Modifier.size(21.dp)
                )
            }
        }

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 10.dp, top = 2.dp)
            )
        }
    }
}

@Composable
private fun ExpressionField(
    value: String,
    caret: Int,
    isError: Boolean,
    onCaretChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.titleMedium
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val padStart = 16.dp
    val caretColor = colors.primary

    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var fieldWidthPx by remember { mutableStateOf(0) }

    val blink by rememberInfiniteTransition(label = "caret").animateFloat(
        initialValue = 1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 470, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "caretAlpha"
    )

    val safeCaret = caret.coerceIn(0, value.length)
    val shown = value.ifEmpty { " " }

    // 光标跑出可视区时把内容滚过去
    LaunchedEffect(safeCaret, value, layout, fieldWidthPx) {
        val l = layout ?: return@LaunchedEffect
        if (fieldWidthPx <= 0) return@LaunchedEffect
        val x = l.getCursorRect(safeCaret.coerceIn(0, shown.length)).left
        val pad = with(density) { padStart.toPx() }
        val left = scroll.value + pad
        val right = scroll.value + fieldWidthPx - pad
        when {
            x < left -> scroll.animateScrollTo((x - pad - 12f).toInt().coerceAtLeast(0))
            x > right -> scroll.animateScrollTo((x - fieldWidthPx + pad + 12f).toInt().coerceAtLeast(0))
        }
    }

    Box(
        modifier = modifier
            .clip(AtomicShapes.Control)
            .background(colors.surface)
            .border(
                1.dp,
                if (isError) colors.error.copy(alpha = 0.75f) else colors.outlineVariant,
                AtomicShapes.Control
            )
            .onSizeChanged { fieldWidthPx = it.width }
            .pointerInput(value) {
                detectTapGestures { pos ->
                    val l = layout ?: return@detectTapGestures
                    val pad = padStart.toPx()
                    val x = pos.x + scroll.value - pad
                    onCaretChange(l.getOffsetForPosition(Offset(x, pos.y)))
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                text = "f(x) = ...",
                style = style,
                color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                modifier = Modifier.padding(start = padStart)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(padStart))
            Text(
                text = shown,
                style = style,
                color = colors.onSurface,
                maxLines = 1,
                softWrap = false,
                onTextLayout = { layout = it },
                modifier = Modifier.drawWithContent {
                    drawContent()
                    val l = layout ?: return@drawWithContent
                    val offset = safeCaret.coerceIn(0, l.layoutInput.text.length)
                    val rect = l.getCursorRect(offset)
                    val inset = 2.dp.toPx()
                    drawRect(
                        color = caretColor.copy(alpha = blink),
                        topLeft = Offset(rect.left - 1.dp.toPx(), rect.top + inset),
                        size = Size(2.dp.toPx(), (rect.bottom - rect.top) - inset * 2)
                    )
                }
            )
            Spacer(Modifier.width(padStart))
        }
    }
}
