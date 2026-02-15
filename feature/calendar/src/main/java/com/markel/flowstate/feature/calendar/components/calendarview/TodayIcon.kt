package com.markel.flowstate.feature.calendar.components.calendarview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun TodayIcon(
    outlineColor: Color,
    dotColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val scale = size.width / 960f

        drawPath(
            path = Path().apply {
                moveTo(200f * scale, 880f * scale)
                quadraticTo(167f * scale, 880f * scale, 143.5f * scale, 856.5f * scale)
                quadraticTo(120f * scale, 833f * scale, 120f * scale, 800f * scale)
                lineTo(120f * scale, 240f * scale)
                quadraticTo(120f * scale, 207f * scale, 143.5f * scale, 183.5f * scale)
                quadraticTo(167f * scale, 160f * scale, 200f * scale, 160f * scale)
                lineTo(240f * scale, 160f * scale)
                lineTo(240f * scale, 80f * scale)
                lineTo(320f * scale, 80f * scale)
                lineTo(320f * scale, 160f * scale)
                lineTo(640f * scale, 160f * scale)
                lineTo(640f * scale, 80f * scale)
                lineTo(720f * scale, 80f * scale)
                lineTo(720f * scale, 160f * scale)
                lineTo(760f * scale, 160f * scale)
                quadraticTo(793f * scale, 160f * scale, 816.5f * scale, 183.5f * scale)
                quadraticTo(840f * scale, 207f * scale, 840f * scale, 240f * scale)
                lineTo(840f * scale, 800f * scale)
                quadraticTo(840f * scale, 833f * scale, 816.5f * scale, 856.5f * scale)
                quadraticTo(793f * scale, 880f * scale, 760f * scale, 880f * scale)
                lineTo(200f * scale, 880f * scale)
                close()
                moveTo(200f * scale, 800f * scale)
                lineTo(760f * scale, 800f * scale)
                lineTo(760f * scale, 400f * scale)
                lineTo(200f * scale, 400f * scale)
                lineTo(200f * scale, 800f * scale)
                close()
                moveTo(200f * scale, 320f * scale)
                lineTo(760f * scale, 320f * scale)
                lineTo(760f * scale, 240f * scale)
                lineTo(200f * scale, 240f * scale)
                lineTo(200f * scale, 320f * scale)
                close()
            },
            color = outlineColor
        )

        // Draw the dot
        drawPath(
            path = Path().apply {
                moveTo(580f * scale, 720f * scale)
                quadraticTo(538f * scale, 720f * scale, 509f * scale, 691f * scale)
                quadraticTo(480f * scale, 662f * scale, 480f * scale, 620f * scale)
                quadraticTo(480f * scale, 578f * scale, 509f * scale, 549f * scale)
                quadraticTo(538f * scale, 520f * scale, 580f * scale, 520f * scale)
                quadraticTo(622f * scale, 520f * scale, 651f * scale, 549f * scale)
                quadraticTo(680f * scale, 578f * scale, 680f * scale, 620f * scale)
                quadraticTo(680f * scale, 662f * scale, 651f * scale, 691f * scale)
                quadraticTo(622f * scale, 720f * scale, 580f * scale, 720f * scale)
                close()
            },
            color = dotColor
        )
    }
}