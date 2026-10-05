package com.markel.flowstate.feature.checkin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Check-in chrome from the "Evening Check-in Flow" design: a header with the
 * back arrow and five question progress segments, plus the fixed bottom pill
 * button. Both live OUTSIDE the step AnimatedContent so they persist across
 * question transitions, exactly like the design's fixed header/footer; their
 * visibility follows the flow (invisible on the greeting, gone on the legacy
 * unexpected-plans/tasks/plan steps).
 */

@Composable
internal fun CheckinFlowHeader(
    currentStep: CheckinStep,
    alpha: Float,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Ordinal doubles as the design's stage index: GREET = 0 … RECAP = 6.
    val index = currentStep.ordinal
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CheckinFlowColors.Background)
            .statusBarsPadding()
            .height(72.dp)
            .padding(start = 14.dp, top = 16.dp, end = 14.dp)
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = alpha > 0.5f,
                    onClick = onBack
                )
                .semantics { contentDescription = "Back" },
            contentAlignment = Alignment.Center
        ) {
            // The design's back chevron: M15 5l-7 7 7 7
            Canvas(modifier = Modifier.size(24.dp)) {
                val stroke = 1.8.dp.toPx()
                val path = Path().apply {
                    moveTo(size.width * 15f / 24f, size.height * 5f / 24f)
                    lineTo(size.width * 8f / 24f, size.height * 12f / 24f)
                    lineTo(size.width * 15f / 24f, size.height * 19f / 24f)
                }
                drawPath(
                    path = path,
                    color = CheckinFlowColors.Accent,
                    style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(5) { i ->
                val filled = index > i
                val color by animateColorAsState(
                    targetValue = if (filled) CheckinFlowColors.Accent else CheckinFlowColors.SegmentIdle,
                    animationSpec = tween(500),
                    label = "progressSegment"
                )
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(3.dp)
                        .background(color, RoundedCornerShape(2.dp))
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        // Balances the back button so the segments sit dead center.
        Spacer(modifier = Modifier.width(44.dp))
    }
}

@Composable
internal fun CheckinFlowBottomBar(
    label: String,
    alpha: Float,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fonts = rememberCheckinFonts()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CheckinFlowColors.Background)
            .navigationBarsPadding()
            .alpha(alpha)
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CheckinFlowColors.Accent,
                contentColor = CheckinFlowColors.OnAccent,
                disabledContainerColor = CheckinFlowColors.Accent,
                disabledContentColor = CheckinFlowColors.OnAccent
            ),
            elevation = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 30.dp)
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = fonts
                )
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
