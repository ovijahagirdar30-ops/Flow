package com.markel.flowstate.feature.checkin

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Greeting screen of the redesigned check-in — "Hey, Ovi. / How are you
 * feeling today?" with a Begin check-in pill. Ported from the design's
 * greeting stage: the three blocks reveal one at a time (500ms / 1500ms /
 * 2800ms), each fading and rising 12px over 900ms ease-out.
 */
@Composable
internal fun CheckinGreetingStep(
    onBegin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fonts = rememberCheckinFonts()
    // Hoisted for the arrow Canvas below — draw scopes are not composable.
    val colors = CheckinFlowColors
    var stage by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        listOf(500L, 1500L, 2800L).forEachIndexed { i, at ->
            delay(at)
            stage = i + 1
        }
    }
    // Each block's reveal progress; design animates 900ms ease-out per stage.
    val r1 by animateFloatAsState(if (stage >= 1) 1f else 0f, tween(900, easing = EaseOut), label = "greetLine1")
    val r2 by animateFloatAsState(if (stage >= 2) 1f else 0f, tween(900, easing = EaseOut), label = "greetLine2")
    val r3 by animateFloatAsState(if (stage >= 3) 1f else 0f, tween(900, easing = EaseOut), label = "greetAction")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CheckinFlowColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 30.dp)
    ) {
        Spacer(modifier = Modifier.height(150.dp))

        Text(
            text = "Hey, Ovi.",
            style = TextStyle(
                fontSize = 44.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.Light,
                fontFamily = fonts,
                letterSpacing = (-0.44).sp,
                color = CheckinFlowColors.Heading
            ),
            modifier = Modifier.greetingReveal(r1)
        )

        Text(
            text = "How are you feeling today?",
            style = TextStyle(
                fontSize = 44.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.Light,
                fontFamily = fonts,
                letterSpacing = (-0.44).sp,
                color = CheckinFlowColors.Greeting
            ),
            modifier = Modifier
                .padding(top = 6.dp)
                .greetingReveal(r2)
        )

        Column(
            modifier = Modifier
                .padding(top = 56.dp)
                .greetingReveal(r3)
        ) {
            Row(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, CheckinFlowColors.Accent.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                    .clickable(enabled = stage >= 3, onClick = onBegin)
                    .padding(start = 22.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Begin check-in",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = fonts,
                        color = CheckinFlowColors.Accent
                    )
                )
                Spacer(modifier = Modifier.width(10.dp))
                // The design's arrow: M5 12h14M13 6l6 6-6 6
                Canvas(modifier = Modifier.size(18.dp)) {
                    val stroke = 1.8.dp.toPx() * (18f / 24f)
                    val line = Path().apply {
                        moveTo(size.width * 5f / 24f, size.height * 12f / 24f)
                        lineTo(size.width * 19f / 24f, size.height * 12f / 24f)
                    }
                    val head = Path().apply {
                        moveTo(size.width * 13f / 24f, size.height * 6f / 24f)
                        lineTo(size.width * 19f / 24f, size.height * 12f / 24f)
                        lineTo(size.width * 13f / 24f, size.height * 18f / 24f)
                    }
                    val strokeStyle = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    drawPath(line, colors.Accent, style = strokeStyle)
                    drawPath(head, colors.Accent, style = strokeStyle)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Five quick questions. About a minute.",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontFamily = fonts,
                    color = CheckinFlowColors.Muted
                )
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** Design's reveal: fade in while rising 12px, 900ms ease-out. */
private fun Modifier.greetingReveal(progress: Float): Modifier = graphicsLayer {
    alpha = progress
    translationY = (1f - progress) * 12.dp.toPx()
}
