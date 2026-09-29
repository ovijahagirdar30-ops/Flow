package com.markel.flowstate.feature.checkin

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.markel.flowstate.core.domain.PlanBlock
import com.markel.flowstate.core.domain.PlanBlockKind
import java.time.LocalTime
import java.util.Locale

/**
 * Editor for one plan block, shared by the Plan tab and the check-in plan
 * step: title, start time (Material3 time picker swapped in place inside the
 * dialog), duration in minutes, and the reason/details line.
 *
 * - `initial == null` → add mode (Save creates a new block).
 * - `initial != null` → edit mode; the block's [PlanBlock.kind] and
 *   [PlanBlock.referenceId] are PRESERVED so a TASK block keeps pointing at
 *   its real task (the tick→task sync depends on it), and a Remove button
 *   appears.
 *
 * Theme-aware via MaterialTheme: dialogs render in their own window, so one
 * styled dialog reads correctly over both the light/dark Plan tab and the
 * black check-in step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanBlockEditorDialog(
    initial: PlanBlock?,
    onConfirm: (PlanBlock) -> Unit,
    onRemove: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var reason by remember(initial) { mutableStateOf(initial?.reason.orEmpty()) }
    var durationText by remember(initial) {
        mutableStateOf(initial?.durationMinutes?.toString() ?: DEFAULT_DURATION)
    }
    var startTime by remember(initial) { mutableStateOf(initial?.startTime ?: DEFAULT_START) }
    var showingTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Hoisted out of the picker branch on purpose: Save lives in the dialog's
    // confirmButton slot, a SEPARATE lambda that cannot see state declared
    // inside `text` — which is exactly why the picked time never reached
    // startTime and Save kept writing the old one. Re-keyed on
    // showingTimePicker so every open re-seeds from the block's current
    // startTime: a pick you back out of cannot leak back in.
    val pickerState = remember(showingTimePicker) {
        val seed = parseHhMm(startTime)
        TimePickerState(seed.hour, seed.minute, DateFormat.is24HourFormat(context))
    }

    val duration = durationText.toIntOrNull()?.coerceIn(1, MAX_DURATION)
    val canSave = title.isNotBlank() && duration != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (initial == null) "Add block" else "Edit block")
        },
        text = {
            if (showingTimePicker) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    TimePicker(state = pickerState)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Title",
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Starts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { showingTimePicker = true }) {
                            Text(
                                text = formatPlanTime(startTime),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    EditorField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = "Duration (minutes)",
                        keyboardType = KeyboardType.Number,
                    )

                    EditorField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = "Details (optional)",
                        singleLine = false,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    // Picker mode: Save applies the picked time and returns
                    // to the form (the Starts row then shows it); the next
                    // Save persists the whole block and closes the dialog.
                    if (showingTimePicker) {
                        startTime = String.format(Locale.ROOT, "%02d:%02d", pickerState.hour, pickerState.minute)
                        showingTimePicker = false
                        return@TextButton
                    }
                    val minutes = duration ?: return@TextButton
                    val old = initial
                    onConfirm(
                        PlanBlock(
                            startTime = startTime,
                            durationMinutes = minutes,
                            title = title.trim(),
                            reason = reason.trim(),
                            kind = old?.kind ?: PlanBlockKind.TASK,
                            referenceId = old?.referenceId,
                        )
                    )
                }
            ) {
                Text("Save", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            Row {
                if (initial != null && onRemove != null && !showingTimePicker) {
                    TextButton(onClick = onRemove) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
                if (showingTimePicker) {
                    TextButton(onClick = { showingTimePicker = false }) {
                        Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    )
}

@Composable
private fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary,
        )
    )
}

/** "21:05" → LocalTime, falling back to 20:00 on malformed input. */
private fun parseHhMm(value: String): LocalTime = runCatching { LocalTime.parse(value) }
    .getOrDefault(LocalTime.of(20, 0))

private const val DEFAULT_DURATION = "30"
private const val DEFAULT_START = "20:00"
private const val MAX_DURATION = 720
