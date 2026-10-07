package com.markel.flowstate.feature.checkin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

/** One question of the redesigned check-in, per the design's five screens. */
internal data class MoodQuestionSpec(
    val eyebrow: String,
    val question: String,
    val lo: String,
    val hi: String
)

internal fun questionSpecFor(step: CheckinStep): MoodQuestionSpec = when (step) {
    CheckinStep.ENERGY -> MoodQuestionSpec("Energy", "How's your energy right now?", "Low", "High")
    CheckinStep.SLEEP -> MoodQuestionSpec("Sleepiness", "And how sleepy are you?", "Wide awake", "Barely awake")
    CheckinStep.STRESS -> MoodQuestionSpec("Stress", "How's your stress level?", "Calm", "Overwhelmed")
    CheckinStep.BODY -> MoodQuestionSpec("Body", "Any headache or discomfort?", "None", "Hard to ignore")
    CheckinStep.MOTIVATION -> MoodQuestionSpec("Motivation", "Last one. How motivated are you?", "Not at all", "Fully ready")
    else -> error("Not a question step: $step")
}

/**
 * A single question screen of the redesigned flow: eyebrow, big question,
 * oversized 0–10 value, the design's custom slider and an optional note field.
 */
@Composable
internal fun MoodQuestionStep(
    spec: MoodQuestionSpec,
    value: Int,
    comment: String,
    noteOpen: Boolean,
    onValueChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onToggleNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fonts = rememberCheckinFonts()
    val hasNote = comment.isNotBlank()
    val noteLabel = when {
        noteOpen -> "Hide note"
        hasNote -> "Edit note"
        else -> "Add a note"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CheckinFlowColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 30.dp)
    ) {
        Spacer(modifier = Modifier.height(78.dp))

        Text(
            text = spec.eyebrow.uppercase(),
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = fonts,
                letterSpacing = 1.56.sp,
                color = CheckinFlowColors.Accent
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = spec.question,
            style = TextStyle(
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Light,
                fontFamily = fonts,
                color = CheckinFlowColors.Heading
            ),
            modifier = Modifier.heightIn(min = 78.dp)
        )

        Spacer(modifier = Modifier.height(44.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value.toString(),
                style = TextStyle(
                    fontSize = 88.sp,
                    lineHeight = 88.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = fonts,
                    letterSpacing = (-2.64).sp,
                    color = CheckinFlowColors.Heading
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "/ 10",
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = fonts,
                    color = CheckinFlowColors.Muted
                ),
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        MoodValueSlider(
            value = value,
            onValueChange = onValueChange,
            label = "${spec.eyebrow} from 0 to 10, ${spec.lo} to ${spec.hi}",
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = spec.lo,
                style = TextStyle(fontSize = 13.sp, fontFamily = fonts, color = CheckinFlowColors.Muted)
            )
            Text(
                text = spec.hi,
                style = TextStyle(fontSize = 13.sp, fontFamily = fonts, color = CheckinFlowColors.Muted)
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        Row(
            modifier = Modifier
                .height(44.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleNote
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (noteOpen) "\u2212" else "+",
                style = TextStyle(
                    fontSize = 20.sp,
                    lineHeight = 20.sp,
                    fontFamily = fonts,
                    color = CheckinFlowColors.Accent
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = noteLabel,
                style = TextStyle(fontSize = 16.sp, fontFamily = fonts, color = CheckinFlowColors.Accent)
            )
        }

        AnimatedVisibility(
            visible = noteOpen,
            enter = expandVertically(tween(380, easing = EaseOut)) + fadeIn(tween(320, delayMillis = 60)),
            exit = shrinkVertically(tween(320)) + fadeOut(tween(200))
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                BasicTextField(
                    value = comment,
                    onValueChange = onCommentChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontFamily = fonts,
                        color = CheckinFlowColors.Body
                    ),
                    cursorBrush = SolidColor(CheckinFlowColors.Accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Note about ${spec.eyebrow.lowercase()}" },
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, CheckinFlowColors.NoteBorder, RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (comment.isEmpty()) {
                                Text(
                                    text = "A few words, if you like",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontFamily = fonts,
                                        color = CheckinFlowColors.Muted
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * The design's custom 0–10 slider: purple fill up to the value, muted track
 * after it, eleven tick dots and a 4px vertical thumb — all on a single
 * 48dp-tall hit area. Positions animate over 140ms like the design.
 */
@Composable
internal fun MoodValueSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(140),
        label = "moodSliderValue"
    )
    // Hoisted for the track Canvas below — draw scopes are not composable.
    val colors = CheckinFlowColors

    Box(
        modifier = modifier
            .semantics { contentDescription = label }
            .pointerInput(Unit) {
                fun valueAt(x: Float): Int {
                    val inset = 9.dp.toPx()
                    val step = (size.width - 2 * inset) / 10f
                    if (step <= 0f) return 0
                    return ((x - inset) / step).roundToInt().coerceIn(0, 10)
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onValueChange(valueAt(down.position.x))
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.none { it.pressed }) break
                        val change = event.changes.first()
                        onValueChange(valueAt(change.position.x))
                        change.consume()
                    }
                }
            }
    ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val inset = 9.dp.toPx()
            val step = (w - 2 * inset) / 10f
            val gap = 8.dp.toPx()
            val x = inset + step * animated

            val trackTop = 10.dp.toPx()
            val trackHeight = 28.dp.toPx()
            val radiusFull = 14.dp.toPx()
            val radiusSmall = 4.dp.toPx()

            // Fill: purple from the left edge up to the thumb.
            val fillWidth = if (value == 0) 0f else (x - gap).coerceAtLeast(0f)
            if (fillWidth > 0f) {
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(Offset(0f, trackTop), Size(fillWidth, trackHeight)),
                            topLeft = CornerRadius(radiusFull),
                            topRight = CornerRadius(radiusSmall),
                            bottomRight = CornerRadius(radiusSmall),
                            bottomLeft = CornerRadius(radiusFull)
                        )
                    )
                }
                drawPath(path, colors.Accent)
            }

            // Track: muted remainder after the thumb.
            val trackLeft = x + gap
            val trackWidth = if (value >= 10) 0f else (w - trackLeft).coerceAtLeast(0f)
            if (trackWidth > 0f) {
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(Offset(trackLeft, trackTop), Size(trackWidth, trackHeight)),
                            topLeft = CornerRadius(radiusSmall),
                            topRight = CornerRadius(radiusFull),
                            bottomRight = CornerRadius(radiusFull),
                            bottomLeft = CornerRadius(radiusSmall)
                        )
                    )
                }
                drawPath(path, colors.Track)
            }

            // Eleven dots: dim behind the value, accent ahead, hidden under the thumb.
            val dotRadius = 3.dp.toPx()
            val dotCenterY = trackTop + trackHeight / 2f
            for (i in 0..10) {
                val cx = inset + step * i
                val color = if (i < animated) colors.DotDim else colors.Accent
                val alpha = if (abs(i - animated) < 0.5f) 0f else 1f
                if (alpha > 0f) {
                    drawCircle(color, radius = dotRadius, center = Offset(cx, dotCenterY), alpha = alpha)
                }
            }

            // Thumb: 4dp vertical bar over the value position.
            val thumbHalf = 2.dp.toPx()
            drawRoundRect(
                color = colors.Accent,
                topLeft = Offset(x - thumbHalf, 0f),
                size = Size(thumbHalf * 2f, size.height),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
}
