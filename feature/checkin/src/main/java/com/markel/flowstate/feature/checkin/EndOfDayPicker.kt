package com.markel.flowstate.feature.checkin

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

private val CheckinAccent = Color(0xFF9C27B0)
private val CheckinDarkSurface = Color(0xFF212121)

/**
 * "Day ends at <time>" row — the end-of-day option on the check-in's mood
 * step. Opens a Material3 time picker and hands the choice back as a
 * minute-of-day (0..1439); [CheckinViewModel] persists it in DataStore and
 * [isPlanExpired] uses it to blank the Plan tab once the chosen time passes.
 * 0 (midnight, the default) is expressed by the date gate alone — see the
 * expiry rule for why a literal 00:00 cutoff would hide the plan all day.
 *
 * Styled like the other check-in screens (black dialog, purple accent,
 * explicit colors everywhere — nothing here inherits LocalContentColor).
 */
@Composable
fun EndOfDayRow(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Day ends at",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        TextButton(onClick = { showPicker = true }) {
            Text(
                text = formatPlanTime(minutes.toHhMm()),
                style = MaterialTheme.typography.bodyMedium,
                color = CheckinAccent
            )
        }
    }

    if (showPicker) {
        EndOfDayPickerDialog(
            initialMinutes = minutes,
            onConfirm = { picked ->
                showPicker = false
                onMinutesChange(picked)
            },
            onDismiss = { showPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndOfDayPickerDialog(
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val timePickerState = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = DateFormat.is24HourFormat(context)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Text("When does your day end?", color = Color.White)
        },
        text = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                TimePicker(
                    state = timePickerState,
                    colors = checkinTimePickerColors()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(timePickerState.hour * 60 + timePickerState.minute)
            }) {
                Text("Save", color = CheckinAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun checkinTimePickerColors() = TimePickerDefaults.colors(
    clockDialColor = CheckinDarkSurface,
    selectorColor = CheckinAccent,
    clockDialSelectedContentColor = Color.White,
    clockDialUnselectedContentColor = Color.Gray,
    timeSelectorSelectedContainerColor = CheckinAccent,
    timeSelectorSelectedContentColor = Color.White,
    timeSelectorUnselectedContainerColor = CheckinDarkSurface,
    timeSelectorUnselectedContentColor = Color.White,
    periodSelectorSelectedContainerColor = CheckinAccent,
    periodSelectorSelectedContentColor = Color.White,
    periodSelectorUnselectedContainerColor = Color.Transparent,
    periodSelectorUnselectedContentColor = Color.White,
    periodSelectorBorderColor = Color(0xFF616161)
)

/** Minute-of-day → zero-padded 24h "HH:mm" for [formatPlanTime]'s 12-hour render. */
private fun Int.toHhMm(): String = String.format(
    Locale.getDefault(), "%02d:%02d", this / 60, this % 60
)
