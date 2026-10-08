package com.markel.flowstate.feature.checkin

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * "Day ends at <time>" row closing the recap — the design's frame-8 row:
 * a hairline divider, then a muted 17px label left and the lavender value
 * right. Opens a Material3 time picker and hands the choice back as a
 * minute-of-day (0..1439); [CheckinViewModel] persists it in DataStore and
 * [isPlanExpired] uses it to blank the Plan tab once the chosen time passes.
 * 0 (midnight, the default) is expressed by the date gate alone — see the
 * expiry rule for why a literal 00:00 cutoff would hide the plan all day.
 *
 * Styled through the check-in design seam ([CheckinFlowColors] /
 * [rememberCheckinFonts]) rather than MaterialTheme text styles, so it sits
 * natively among the recap rows; the picker dialog itself stays themed by
 * the app's color scheme like every other picker.
 */
@Composable
fun EndOfDayRow(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val fonts = rememberCheckinFonts()
    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
    ) {
        HorizontalDivider(color = CheckinFlowColors.NoteBorder, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Day ends at",
                style = TextStyle(
                    fontSize = 17.sp,
                    fontFamily = fonts,
                    color = CheckinFlowColors.Muted
                )
            )
            Row(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showPicker = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatPlanTime(minutes.toHhMm()),
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontFamily = fonts,
                        color = CheckinFlowColors.Accent
                    )
                )
            }
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
        title = {
            Text("When does your day end?")
        },
        text = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Default TimePicker colors: derived from the theme, so the
                // dial follows light/dark and the AppColor like every other
                // picker in the app.
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(timePickerState.hour * 60 + timePickerState.minute)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/** Minute-of-day → zero-padded 24h "HH:mm" for [formatPlanTime]'s 12-hour render. */
private fun Int.toHhMm(): String = String.format(
    Locale.getDefault(), "%02d:%02d", this / 60, this % 60
)
