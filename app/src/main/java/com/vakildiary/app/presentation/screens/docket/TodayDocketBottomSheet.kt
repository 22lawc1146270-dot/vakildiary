package com.vakildiary.app.presentation.screens.docket

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vakildiary.app.presentation.viewmodels.DocketItem
import com.vakildiary.app.presentation.viewmodels.DocketType
import com.vakildiary.app.presentation.viewmodels.DocketUiState
import com.vakildiary.app.presentation.theme.VakilTheme
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VakilTheme.colors.bgSecondary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(VakilTheme.colors.bgSurfaceSoft)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VakilTheme.spacing.md)
        ) {
            Header(uiState = uiState, onDismiss = onDismiss)
            Spacer(modifier = Modifier.height(VakilTheme.spacing.md))

            when (uiState) {
                DocketUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = VakilTheme.colors.accentPrimary)
                    }
                }
                is DocketUiState.Error -> {
                    Text(text = uiState.message, color = VakilTheme.colors.error, modifier = Modifier.padding(VakilTheme.spacing.md))
                }
                is DocketUiState.Success -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(VakilTheme.spacing.sm)
                    ) {
                        item {
                            SectionHeader(title = "HEARINGS", count = uiState.hearings.size)
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
                            Spacer(modifier = Modifier.height(VakilTheme.spacing.md))
                            SectionHeader(title = "TASKS", count = uiState.tasks.size)
                        }
                        items(uiState.tasks) { item ->
                            DocketRow(
                                item = item,
                                advocateName = uiState.advocateName,
                                onToggle = { checked -> onToggleTask(item.id, checked) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(VakilTheme.spacing.xl)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(uiState: DocketUiState, onDismiss: () -> Unit) {
    val dateText = remember {
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")
        LocalDate.now().format(formatter)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Today's Docket", style = VakilTheme.typography.headlineMedium, color = VakilTheme.colors.textPrimary)
            Text(text = dateText, style = VakilTheme.typography.bodyMedium, color = VakilTheme.colors.textSecondary)
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (uiState is DocketUiState.Success) {
                Surface(
                    color = VakilTheme.colors.accentSoft,
                    shape = CircleShape
                ) {
                    Text(
                        text = "${uiState.completedCount}/${uiState.totalCount} DONE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = VakilTheme.typography.labelSmall,
                        color = VakilTheme.colors.accentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(VakilTheme.spacing.sm))
            IconButton(
                onClick = onDismiss,
                colors = IconButtonDefaults.iconButtonColors(containerColor = VakilTheme.colors.bgElevated)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = VakilTheme.colors.textPrimary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = VakilTheme.spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = VakilTheme.typography.labelSmall,
            color = VakilTheme.colors.accentPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$count Items",
            style = VakilTheme.typography.labelSmall,
            color = VakilTheme.colors.textTertiary
        )
    }
}

@Composable
private fun DocketRow(
    item: DocketItem,
    advocateName: String,
    onToggle: (Boolean) -> Unit
) {
    val strikeProgress by animateFloatAsState(targetValue = if (item.isCompleted) 1f else 0f, label = "Strike")
    val textColor = if (item.isCompleted) {
        VakilTheme.colors.textTertiary
    } else {
        VakilTheme.colors.textPrimary
    }
    val subtitleColor = if (item.isOverdue && !item.isCompleted) {
        VakilTheme.colors.error
    } else {
        VakilTheme.colors.textSecondary
    }
    val context = LocalContext.current

    Surface(
        color = if (item.isCompleted) Color.Transparent else VakilTheme.colors.bgElevated,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(VakilTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isCompleted,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = VakilTheme.colors.accentPrimary,
                    uncheckedColor = VakilTheme.colors.textTertiary
                )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = VakilTheme.spacing.xs)
            ) {
                Text(
                    text = item.title,
                    style = VakilTheme.typography.bodyLarge,
                    color = textColor,
                    fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.drawStrikeThrough(strikeProgress, textColor)
                )
                Text(
                    text = item.subtitle,
                    style = VakilTheme.typography.labelSmall,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                        tint = VakilTheme.colors.accentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (item.type == DocketType.HEARING) VakilTheme.colors.accentPrimary else VakilTheme.colors.warning)
            )
        }
    }
}

private fun Modifier.drawStrikeThrough(progress: Float, color: Color): Modifier {
    if (progress <= 0f) return this
    return drawBehind {
        val y = size.height / 2f
        drawLine(
            color = color.copy(alpha = 0.6f),
            start = androidx.compose.ui.geometry.Offset(0f, y),
            end = androidx.compose.ui.geometry.Offset(size.width * progress, y),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}
