package com.markel.flowstate.feature.calendar.components.calendarview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kizitonwose.calendar.core.WeekDay

@Composable
fun WeekDayCell(
    day: WeekDay,
    isSelected: Boolean,
    hasTasks: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1.4f)
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.tertiary
                        isToday -> MaterialTheme.colorScheme.surfaceVariant
                        else -> Color.Transparent
                    }
                )
                .clickable(onClick = onClick)
        ) {

            Text(
                modifier = Modifier.align(Alignment.Center),
                text = day.date.dayOfMonth.toString(),
                fontSize = 15.sp,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onTertiary
                else if (isToday)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
            )

            // Dot if there are tasks
            if (hasTasks) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 5.dp)
                        .size(3.5.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.onTertiary
                            else
                                MaterialTheme.colorScheme.tertiary
                        )
                )
            }
        }
    }
}