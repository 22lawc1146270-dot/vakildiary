package com.vakildiary.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vakildiary.app.presentation.theme.VakilTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            modifier = modifier,
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = VakilTheme.colors.bgElevated,
                contentColor = VakilTheme.colors.textPrimary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = VakilTheme.colors.bgElevated,
                contentColor = VakilTheme.colors.textPrimary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            content = content
        )
    }
}

@Composable
fun UrgencyBadge(daysRemaining: Int) {
    val (containerColor, contentColor) = when {
        daysRemaining <= 2 -> VakilTheme.colors.error to VakilTheme.colors.onAccent
        daysRemaining <= 7 -> VakilTheme.colors.warning to VakilTheme.colors.onAccent
        else -> VakilTheme.colors.accentSoft to VakilTheme.colors.accentPrimary
    }

    Surface(
        shape = RoundedCornerShape(100.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = when {
                daysRemaining < 0 -> "Overdue"
                daysRemaining == 0 -> "Today"
                daysRemaining == 1 -> "Tomorrow"
                else -> "$daysRemaining days"
            },
            style = VakilTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
