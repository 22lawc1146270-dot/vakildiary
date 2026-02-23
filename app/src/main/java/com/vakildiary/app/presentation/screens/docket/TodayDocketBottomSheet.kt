package com.vakildiary.app.presentation.screens.docket

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vakildiary.app.presentation.viewmodels.DocketItem
import com.vakildiary.app.presentation.viewmodels.DocketType
import com.vakildiary.app.presentation.viewmodels.DocketUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import com.vakildiary.app.core.ShareUtils
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayDocketBottomSheet(
    uiState: DocketUiState,
    onDismiss: () -> Unit,
    onToggleHearing: (hearingId: String, isCompleted: Boolean) -> Unit,
    onHearingOutcome: (hearingId: String) -> Unit,
    onToggleTask: (taskId: String, isCompleted: Boolean) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
) {
    val handleColor = MaterialTheme.colorScheme.onSurfaceVariant
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .drawBehind {
                        drawRect(
                            color = handleColor,
                            size = size
                        )
                    }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Header(uiState = uiState, onDismiss = onDismiss)
            Spacer(modifier = Modifier.height(12.dp))

            when (uiState) {
                DocketUiState.Loading -> {
                    Text(text = "Loading...", style = MaterialTheme.typography.bodyMedium)
                }
                is DocketUiState.Error -> {
                    Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
                }
                is DocketUiState.Success -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            SectionHeader(title = "HEARINGS (${uiState.hearings.size})")
                        }
                        items(uiState.hearings) { item ->
                            DocketRow(
                                item = item,
                                advocateName = uiState.advocateName,
                                onToggle = { checked ->
                                    if (checked) {
                                        onHearingOutcome(item.id)
                                    } else {
                                        onToggleHearing(item.id, checked)
                                    }
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            SectionHeader(title = "TASKS (${uiState.tasks.size})")
                        }
                        items(uiState.tasks) { item ->
                            DocketRow(
                                item = item,
                                advocateName = uiState.advocateName,
                                onToggle = { checked -> onToggleTask(item.id, checked) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(uiState: DocketUiState, onDismiss: () -> Unit) {
    val dateText = remember {
        val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")
        LocalDate.now().format(formatter)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Today's Docket", style = MaterialTheme.typography.titleLarge)
            Text(text = dateText, style = MaterialTheme.typography.bodyMedium)
        }
        AssistChip(
            onClick = {},
            label = {
                val progressText = when (uiState) {
                    is DocketUiState.Success -> "${uiState.completedCount} of ${uiState.totalCount} done"
                    else -> "0 of 0 done"
                }
                Text(text = progressText)
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun DocketRow(
    item: DocketItem,
    advocateName: String,
    onToggle: (Boolean) -> Unit
) {
    val strikeProgress by animateFloatAsState(targetValue = if (item.isCompleted) 1f else 0f)
    val textColor = if (item.isCompleted) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = if (item.isOverdue && !item.isCompleted) {
        MaterialTheme.colorScheme.error
    } else {
        textColor
    }
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isCompleted,
            onCheckedChange = onToggle
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.drawStrikeThrough(strikeProgress, textColor)
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.isOverdue && !item.isCompleted) {
                Text(
                    text = "Overdue",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        if (item.type == DocketType.HEARING && item.nextHearingDate != null) {
            IconButton(onClick = {
                ShareUtils.shareHearingDateText(
                    context = context,
                    clientName = item.clientName ?: "Client",
                    caseName = item.title,
                    date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    court = item.courtName ?: "",
                    advocateName = advocateName
                )
            }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        DocketChip(type = item.type)
    }
}

@Composable
private fun DocketChip(type: DocketType) {
    val (label, color) = when (type) {
        DocketType.HEARING -> "Hearing" to Color(0xFF1565C0)
        DocketType.TASK -> "Task" to Color(0xFFE67E22)
    }

    AssistChip(
        onClick = {},
        label = { Text(text = label) },
        leadingIcon = {
            Canvas(modifier = Modifier.size(8.dp)) {
                drawCircle(color = color)
            }
        }
    )
}

private fun Modifier.drawStrikeThrough(progress: Float, color: Color): Modifier {
    if (progress <= 0f) return this
    return drawBehind {
        val y = size.height / 2f
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, y),
            end = androidx.compose.ui.geometry.Offset(size.width * progress, y),
            strokeWidth = 2.dp.toPx()
        )
    }
}
