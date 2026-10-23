package com.yumoo.functionplotter.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yumoo.functionplotter.data.model.PlotFunction
import com.yumoo.functionplotter.ui.theme.AtomicShapes

@Composable
fun FunctionChips(
    functions: List<PlotFunction>,
    selectedId: Long,
    onSelect: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onAdd: () -> Unit,
    canAdd: Boolean,
    modifier: Modifier = Modifier,
    invalidIds: Set<Long> = emptySet()
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
    ) {
        items(functions, key = { it.id }) { function ->
            val selected = function.id == selectedId
            val broken = function.id in invalidIds
            val container by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                animationSpec = tween(180),
                label = "chipContainer"
            )
            Box(
                modifier = Modifier
                    .clip(AtomicShapes.Chip)
                    .background(container)
                    .border(
                        width = 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = AtomicShapes.Chip
                    )
                    .clickable { onSelect(function.id) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(Color(function.colorArgb), CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    if (broken) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "表达式有误",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(13.dp)
                                .padding(end = 2.dp)
                        )
                    }
                    Text(
                        function.expression.ifBlank { "(空)" },
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )
                    if (functions.size > 1) {
                        IconButton(
                            onClick = { onRemove(function.id) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "删除",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
        if (canAdd) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.clickable { onAdd() }
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "新增",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("新增", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
