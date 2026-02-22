package com.vakildiary.app.presentation.screens.docket

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

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
        title = { Text(text = "Record Hearing Outcome") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = caseName)

                OutlinedTextField(
                    value = outcome,
                    onValueChange = { outcome = it },
                    label = { Text(text = "What happened today?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                OutlinedTextField(
                    value = orderDetails,
                    onValueChange = { orderDetails = it },
                    label = { Text(text = "Order details") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                OutlinedTextField(
                    value = adjournmentReason,
                    onValueChange = { adjournmentReason = it },
                    label = { Text(text = "Adjournment reason") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = onAddVoiceNote,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Add voice note"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Add voice note instead")
                }
                if (!voiceNotePath.isNullOrBlank()) {
                    Text(text = "Voice note selected")
                }

                OutlinedTextField(
                    value = nextDate,
                    onValueChange = { nextDate = it },
                    label = { Text(text = "Next hearing date? (DD/MM/YYYY)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSaveAndMarkDone(outcome, orderDetails, adjournmentReason, nextDate) }) {
                Text(text = "Save & Mark Done")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onSkipAndMarkDone(outcome, orderDetails, adjournmentReason, nextDate) }) {
                    Text(text = "Skip & Mark Done")
                }
                TextButton(onClick = onDismiss) {
                    Text(text = "Cancel")
                }
            }
        }
    )
}
