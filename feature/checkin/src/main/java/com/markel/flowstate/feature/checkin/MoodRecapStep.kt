package com.markel.flowstate.feature.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One recap row: question label, note marker and the chosen value. */
internal data class RecapRow(
    val step: CheckinStep,
    val label: String,
    val value: Int,
    val hasNote: Boolean
)

/**
 * Recap screen ending the designed flow — "Thanks, Ovi." plus the five
 * answers as tappable rows (a "· note" marker when a question carries a
 * note), so any answer can be amended before the plan is generated, closed
 * by the design's "Day ends at" row (the end-of-day cutoff for the Plan tab).
 */
@Composable
internal fun MoodRecapStep(
    rows: List<RecapRow>,
    endOfDayMinutes: Int,
    onEndOfDayChange: (Int) -> Unit,
    onAmend: (CheckinStep) -> Unit,
    modifier: Modifier = Modifier
) {
    val fonts = rememberCheckinFonts()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CheckinFlowColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 30.dp)
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "Thanks, Ovi.",
            style = TextStyle(
                fontSize = 44.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.Light,
                fontFamily = fonts,
                letterSpacing = (-0.44).sp,
                color = CheckinFlowColors.Heading
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "That's enough for me to shape your evening.",
            style = TextStyle(
                fontSize = 22.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Light,
                fontFamily = fonts,
                color = CheckinFlowColors.Greeting
            )
        )

        Spacer(modifier = Modifier.height(44.dp))

        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onAmend(row.step) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row.label,
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontFamily = fonts,
                            color = CheckinFlowColors.Muted
                        )
                    )
                    if (row.hasNote) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "\u00b7 note",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontFamily = fonts,
                                color = CheckinFlowColors.NoteMuted
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = row.value.toString(),
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontFamily = fonts,
                        color = CheckinFlowColors.Heading
                    )
                )
            }
        }

        // Design's frame-8 closing row: hairline divider, then the
        // end-of-day cutoff that gates the Plan tab's visibility.
        EndOfDayRow(
            minutes = endOfDayMinutes,
            onMinutesChange = onEndOfDayChange
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
