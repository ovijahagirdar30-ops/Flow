package com.markel.flowstate.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markel.flowstate.R
import java.time.LocalDate

@Composable
fun DynamicCalendarIcon() {
    val today = remember { LocalDate.now().dayOfMonth.toString() }
    val fontSize = remember(today) { if (today.length == 1) 12.sp else 10.sp }

    Box(contentAlignment = Alignment.Center) {
        // Base icon
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.calendar_today),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            // Number day
            Text(
                text = today,
                style = TextStyle(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    // Delete extra horizontal spacing
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    ),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    )
                )
            )
        }
    }
}