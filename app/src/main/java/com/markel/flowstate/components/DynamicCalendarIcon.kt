package com.markel.flowstate.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markel.flowstate.R
import java.time.LocalDate

@Composable
fun DynamicCalendarIcon() {
    val today = remember { LocalDate.now().dayOfMonth.toString() }

    Box(contentAlignment = Alignment.Center) {
        // Base icon
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.calendar_today),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )

        // Number day
        Text(
            text = today,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            // Adjust size to fit it inside the icon
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp, start = 1.dp)
        )
    }
}