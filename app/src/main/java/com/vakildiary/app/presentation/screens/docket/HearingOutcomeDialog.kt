package com.vakildiary.app.presentation.screens.docket

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vakildiary.app.presentation.theme.VakilTheme

@Composable
fun HearingOutcomeDialog(
    caseName: String,
    voiceNotePath: String?,
    onDismiss: () -> Unit,
    onAddVoiceNote: () -> Unit,
    onSkipAndMarkDone: (outcome: String, orderDetails: String, adjournmentReason: String, nextDate: String) -> Unit,
    onSaveAndMarkDone: (outcome: String, orderDetails: String, adjournmentReason: String, nextDate: String) -> Unit
) {
    var outcome by remember { mutableStateOf("") }
    var orderDetails by remember { mutableStateOf("") }
    var adjournmentReason by remember { mutableStateOf("") }
    var nextDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VakilTheme.colors.bgElevated,
        titleContentColor = VakilTheme.colors.textPrimary,
        textContentColor = VakilTheme.colors.textSecondary,
        title = { 
            Text(
                text = "Hearing Summary", 
                style = VakilTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VakilTheme.spacing.md)) {
                Text(
                    text = caseName, 
                    style = VakilTheme.typography.bodyLarge,
                    color = VakilTheme.colors.accentPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                AppDialogTextField(
                    value = outcome,
                    onValueChange = { outcome = it },
                    label = "Proceedings Summary",
                    minLines = 2
                )

                AppDialogTextField(
                    value = orderDetails,
                    onValueChange = { orderDetails = it },
                    label = "Order Details",
                    minLines = 2
                )

                AppDialogTextField(
                    value = adjournmentReason,
                    onValueChange = { adjournmentReason = it },
                    label = "Adjournment Reason"
                )

                OutlinedButton(
                    onClick = onAddVoiceNote,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VakilTheme.colors.accentPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(VakilTheme.colors.bgSurfaceSoft))
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice note",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Add Voice Note", style = VakilTheme.typography.labelSmall)
                }
                
                if (!voiceNotePath.isNullOrBlank()) {
                    Text(text = "✓ Voice note attached", color = VakilTheme.colors.success, style = VakilTheme.typography.labelSmall)
                }

                AppDialogTextField(
                    value = nextDate,
                    onValueChange = { nextDate = it },
                    label = "Next Hearing (DD/MM/YYYY)",
                    keyboardType = KeyboardType.Number
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveAndMarkDone(outcome, orderDetails, adjournmentReason, nextDate) },
                colors = ButtonDefaults.buttonColors(containerColor = VakilTheme.colors.accentPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "Save & Complete", style = VakilTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(VakilTheme.spacing.xs)) {
                TextButton(onClick = { onSkipAndMarkDone(outcome, orderDetails, adjournmentReason, nextDate) }) {
                    Text(text = "Skip", color = VakilTheme.colors.textSecondary, style = VakilTheme.typography.labelSmall)
                }
                TextButton(onClick = onDismiss) {
                    Text(text = "Cancel", color = VakilTheme.colors.textTertiary, style = VakilTheme.typography.labelSmall)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, style = VakilTheme.typography.labelSmall) },
        modifier = Modifier.fillMaxWidth(),
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VakilTheme.colors.accentPrimary,
            unfocusedBorderColor = VakilTheme.colors.bgSurfaceSoft,
            focusedTextColor = VakilTheme.colors.textPrimary,
            unfocusedTextColor = VakilTheme.colors.textPrimary,
            focusedLabelColor = VakilTheme.colors.accentPrimary,
            unfocusedLabelColor = VakilTheme.colors.textTertiary
        ),
        textStyle = VakilTheme.typography.bodyMedium
    )
}
