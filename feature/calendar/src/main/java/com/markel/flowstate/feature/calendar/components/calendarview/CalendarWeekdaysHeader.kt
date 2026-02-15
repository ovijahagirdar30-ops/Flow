package com.markel.flowstate.feature.calendar.components.calendarview

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarWeekdaysHeader(){
    Row(modifier = Modifier.fillMaxWidth()) {
        val daysOfWeek = remember { firstDayOfWeekFromLocale().let { first -> (0..6).map { first.plus(it.toLong()) } } }
        for (day in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}