package com.markel.flowstate.feature.checkin

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * The violet light beam behind the planning and settled screens of the
 * "Planning to Plan" design — a soft S-ribbon drawn as stacked bezier strokes
 * (widest and faintest first). Layered widths stand in for a blur so the
 * glow renders identically on every API level.
 */
@Composable
internal fun CheckinAurora(
    modifier: Modifier = Modifier,
    strength: Float = 1f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val ribbon = Path().apply {
            moveTo(w * 0.44f, -h * 0.06f)
            cubicTo(w * 0.68f, h * 0.18f, w * 0.30f, h * 0.34f, w * 0.56f, h * 0.64f)
        }
        // Widest/faintest first so the core stays the brightest layer.
        val layers = listOf(
            0.55f to 0.05f,
            0.36f to 0.08f,
            0.22f to 0.13f,
            0.12f to 0.22f,
            0.05f to 0.45f
        )
        layers.forEach { (widthFrac, alphaFrac) ->
            drawPath(
                path = ribbon,
                color = CheckinFlowColors.Aurora.copy(alpha = alphaFrac * strength),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = w * widthFrac,
                    cap = StrokeCap.Round
                )
            )
        }
        // Broad halo hugging the top of the beam.
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    CheckinFlowColors.Aurora.copy(alpha = 0.30f * strength),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.06f),
                radius = w * 0.62f
            )
        )
    }
}

/** The rotating status lines of the planning screen, in design order. */
private val planningStatuses = listOf(
    "Looking at how today went.",
    "Finding a shape for tonight.",
    "Placing calm where it matters.",
    "Pulling your evening together."
)

/**
 * Frames 2a–2d of the "Planning to Plan" design: full-bleed aurora with
 * "Planning your evening" anchored low, a status line rotating beneath it
 * and skeleton rows filling in one per status — the shape of the list that
 * is about to arrive.
 */
@Composable
internal fun CheckinPlanningStep(
    modifier: Modifier = Modifier
) {
    val fonts = rememberCheckinFonts()
    var statusIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_700)
            statusIndex = (statusIndex + 1) % planningStatuses.size
        }
    }
    val skeletonRows = (statusIndex + 1).coerceAtMost(planningStatuses.size)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CheckinFlowColors.Background)
            .navigationBarsPadding()
    ) {
        CheckinAurora(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
        ) {
            Text(
                text = "Planning your evening",
                style = TextStyle(
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = fonts,
                    color = CheckinFlowColors.Heading
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Crossfade(targetState = statusIndex, animationSpec = tween(500), label = "planningStatus") { i ->
                Text(
                    text = planningStatuses[i],
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontFamily = fonts,
                        color = CheckinFlowColors.Greeting
                    )
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Fixed slots keep the title from riding up as rows appear; each
            // slot fades its skeleton in the moment it is "fetched".
            repeat(planningStatuses.size) { i ->
                val shown = i < skeletonRows
                val alpha by animateFloatAsState(
                    targetValue = if (shown) 1f else 0f,
                    animationSpec = tween(400),
                    label = "skeletonAlpha"
                )
                val offsetY by animateDpAsState(
                    targetValue = if (shown) 0.dp else 14.dp,
                    animationSpec = tween(400),
                    label = "skeletonOffset"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .alpha(alpha)
                            .padding(top = offsetY)
                            .fillMaxWidth()
                    ) {
                        SkeletonTaskRow(fonts = fonts)
                    }
                }
            }

            Spacer(modifier = Modifier.height(44.dp))
        }
    }
}

/** One placeholder list row: hollow circle, title bar, right-aligned time bar. */
@Composable
private fun SkeletonTaskRow(fonts: androidx.compose.ui.text.font.FontFamily) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(
                    width = 1.5.dp,
                    color = CheckinFlowColors.Track,
                    shape = RoundedCornerShape(10.dp)
                )
        )
        Spacer(modifier = Modifier.width(18.dp))
        // A slow shimmer so the skeleton reads as "working".
        val transition = rememberInfiniteTransition(label = "skeleton")
        val pulse by transition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "skeletonPulse"
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .background(
                    CheckinFlowColors.Track.copy(alpha = pulse),
                    RoundedCornerShape(6.dp)
                )
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(10.dp)
                .background(
                    CheckinFlowColors.DotDim.copy(alpha = pulse),
                    RoundedCornerShape(5.dp)
                )
        )
    }
}
