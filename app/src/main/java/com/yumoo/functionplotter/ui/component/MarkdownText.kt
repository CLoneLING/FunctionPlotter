package com.yumoo.functionplotter.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 极简 Markdown 渲染：标题、段落、无序列表、引用、代码块、分割线，
 * 行内支持 **粗体**、*斜体*、`代码`、[链接](url)。
 *
 * 只覆盖更新日志与关于页实际会用到的语法，不引入第三方渲染库。
 */
sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class Bullets(val items: List<String>) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data class Code(val code: String) : MarkdownBlock
    data object Divider : MarkdownBlock
}

fun parseMarkdown(source: String): List<MarkdownBlock> {
    val blocks = ArrayList<MarkdownBlock>()
    val lines = source.replace("\r\n", "\n").split('\n')
    var index = 0
    val paragraph = StringBuilder()

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(paragraph.toString()))
            paragraph.clear()
        }
    }

    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() -> {
                flushParagraph()
                index++
            }
            trimmed.startsWith("```") -> {
                flushParagraph()
                index++
                val code = StringBuilder()
                while (index < lines.size && !lines[index].trim().startsWith("```")) {
                    code.appendLine(lines[index])
                    index++
                }
                if (index < lines.size) index++
                blocks.add(MarkdownBlock.Code(code.toString().trimEnd()))
            }
            trimmed.startsWith("#") -> {
                flushParagraph()
                val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 3)
                blocks.add(MarkdownBlock.Heading(level, trimmed.drop(level).trim()))
                index++
            }
            trimmed == "---" || trimmed == "***" -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Divider)
                index++
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                flushParagraph()
                val items = ArrayList<String>()
                while (index < lines.size) {
                    val itemLine = lines[index].trim()
                    if (itemLine.startsWith("- ") || itemLine.startsWith("* ")) {
                        items.add(itemLine.drop(2).trim())
                        index++
                    } else if (itemLine.isEmpty()) {
                        index++
                        break
                    } else {
                        break
                    }
                }
                blocks.add(MarkdownBlock.Bullets(items))
            }
            trimmed.startsWith("> ") -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Quote(trimmed.drop(2).trim()))
                index++
            }
            else -> {
                if (paragraph.isNotEmpty()) paragraph.append(' ')
                paragraph.append(trimmed)
                index++
            }
        }
    }
    flushParagraph()
    return blocks
}

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    baseStyle: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val blocks = remember(markdown) { parseMarkdown(markdown) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> Text(
                    text = inlineMarkdown(block.text, textColor, linkColor, codeBackground),
                    style = when (block.level) {
                        1 -> baseStyle.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        2 -> baseStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        else -> baseStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                )

                is MarkdownBlock.Paragraph -> Text(
                    text = inlineMarkdown(block.text, textColor, linkColor, codeBackground),
                    style = baseStyle
                )

                is MarkdownBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    block.items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("·", style = baseStyle, color = textColor.copy(alpha = 0.5f))
                            Text(
                                text = inlineMarkdown(item, textColor, linkColor, codeBackground),
                                style = baseStyle,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }

                is MarkdownBlock.Quote -> Text(
                    text = inlineMarkdown(block.text, textColor.copy(alpha = 0.8f), linkColor, codeBackground),
                    style = baseStyle.copy(fontStyle = FontStyle.Italic)
                )

                is MarkdownBlock.Code -> Text(
                    text = block.code,
                    style = baseStyle.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(codeBackground, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                        .horizontalScroll(rememberScrollState())
                )

                MarkdownBlock.Divider -> Text(
                    text = "────────",
                    style = baseStyle,
                    color = textColor.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun inlineMarkdown(
    text: String,
    color: Color,
    linkColor: Color,
    codeBackground: Color
): AnnotatedString = buildAnnotatedString {
    var index = 0
    while (index < text.length) {
        when {
            text.startsWith("**", index) -> {
                val end = text.indexOf("**", index + 2)
                if (end > index) {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(text.substring(index + 2, end))
                    }
                    index = end + 2
                } else {
                    append(text[index]); index++
                }
            }
            text[index] == '`' -> {
                val end = text.indexOf('`', index + 1)
                if (end > index) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            background = codeBackground
                        )
                    ) {
                        append(text.substring(index + 1, end))
                    }
                    index = end + 1
                } else {
                    append(text[index]); index++
                }
            }
            text[index] == '[' -> {
                val labelEnd = text.indexOf("](", index)
                val urlEnd = if (labelEnd > 0) text.indexOf(')', labelEnd + 2) else -1
                if (labelEnd > 0 && urlEnd > labelEnd) {
                    val label = text.substring(index + 1, labelEnd)
                    val url = text.substring(labelEnd + 2, urlEnd)
                    withLink(
                        LinkAnnotation.Url(
                            url = url,
                            styles = TextLinkStyles(style = SpanStyle(color = linkColor))
                        )
                    ) { append(label) }
                    index = urlEnd + 1
                } else {
                    append(text[index]); index++
                }
            }
            text[index] == '*' -> {
                val end = text.indexOf('*', index + 1)
                if (end > index) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(index + 1, end))
                    }
                    index = end + 1
                } else {
                    append(text[index]); index++
                }
            }
            else -> {
                append(text[index])
                index++
            }
        }
    }
    addStyle(SpanStyle(color = color), 0, length)
}
