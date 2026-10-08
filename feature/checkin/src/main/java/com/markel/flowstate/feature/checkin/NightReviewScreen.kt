package com.markel.flowstate.feature.checkin

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markel.flowstate.core.domain.Task
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val reviewDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
private val dueDateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

// Stagger of the design's entrance: each block fades in and rises 12px
// over 900ms ease-out, at these delays from page load.
private const val RV_HEADER = 300L
private const val RV_HERO = 700L
private const val RV_LEAD = 1_400L
private const val RV_COMPLETED = 2_000L
private const val RV_OPEN_HEADER = 2_500L
private const val RV_ITEM_BASE = 2_700L
private const val RV_ITEM_STEP = 150L
private const val RV_ITEM_MAX = 3_450L
private const val RV_BAR = 3_600L

/**
 * The 9PM night check-in, ported from the design's night-checkin screen:
 * an eyebrow/date header, the hero line (lavender-soft second half), an
 * "N items are waiting" lead, Completed-today and Still-open blocks, and a
 * bordered bottom bar with a "Done for today" pill. Tapping the pill fades
 * the content up and out while a soft planning-light haze blooms bottom
 * left, revealing the closing "Good night" state; Back returns.
 *
 * Reached only from CheckinActivity in night-review mode (the 9PM alarm,
 * or the debug openNight hook); not a bottom-nav destination. Runs inside
 * FlowStateTheme — palette and fonts come from the shared check-in design
 * seam ([CheckinFlowColors] / [rememberCheckinFonts]), never hex values.
 */
@Composable
fun NightReviewScreen(
    onDone: () -> Unit,
    viewModel: NightReviewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val review = state.review
    val fonts = rememberCheckinFonts()
    val colors = CheckinFlowColors

    var closing by remember { mutableStateOf(false) }
    // System back: the design's Back link leaves the closing state; with the
    // content showing, back means leaving the page entirely (onDone).
    BackHandler {
        if (closing) closing = false else onDone()
    }

    val haze by animateFloatAsState(
        targetValue = if (closing) 1f else 0f,
        animationSpec = tween(2_200, delayMillis = 400),
        label = "nightHaze"
    )
    val contentFade by animateFloatAsState(
        targetValue = if (closing) 0f else 1f,
        animationSpec = tween(400),
        label = "nightContent"
    )
    val closingFade by animateFloatAsState(
        targetValue = if (closing) 1f else 0f,
        animationSpec = tween(900, delayMillis = 450),
        label = "nightClosing"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.Background)
    ) {
        // Quiet echo of the planning light, only shown at the end of the day.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-70).dp, y = 220.dp)
                .size(width = 530.dp, height = 430.dp)
                .graphicsLayer { alpha = haze * 0.38f }
                .background(
                    Brush.radialGradient(
                        listOf(colors.Accent.copy(alpha = 0.9f), Color.Transparent)
                    )
                )
        )

        if (review == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val openItems = review.pending + review.pushedToTomorrow

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = contentFade
                        // Design: content drifts 8px upward as it fades out.
                        translationY = (1f - contentFade) * -8.dp.toPx()
                    }
                    .statusBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 30.dp)
                ) {
                    Spacer(modifier = Modifier.height(32.dp))

                    // ── Header ─────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .nightReveal(revealAt(RV_HEADER)),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "NIGHT CHECK-IN",
                            modifier = Modifier.alignBy(FirstBaseline),
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = fonts,
                                letterSpacing = 1.56.sp,
                                color = colors.Accent
                            )
                        )
                        Text(
                            text = formatDate(review.date),
                            modifier = Modifier.alignBy(FirstBaseline),
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontFamily = fonts,
                                color = colors.Muted
                            )
                        )
                    }

                    // ── Hero ───────────────────────────────────────────────
                    Text(
                        text = heroText(review.completedToday.size, colors.Greeting),
                        style = TextStyle(
                            fontSize = 26.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = fonts,
                            letterSpacing = (-0.13).sp,
                            color = colors.Heading
                        ),
                        modifier = Modifier
                            .padding(top = 40.dp)
                            .fillMaxWidth()
                            .nightReveal(revealAt(RV_HERO))
                    )

                    Text(
                        text = leadText(openItems.size),
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            fontFamily = fonts,
                            color = colors.Muted
                        ),
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .nightReveal(revealAt(RV_LEAD))
                    )

                    // ── Completed today ────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .padding(top = 34.dp)
                            .fillMaxWidth()
                            .nightReveal(revealAt(RV_COMPLETED))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SectionEyebrow(
                                text = "COMPLETED TODAY",
                                fonts = fonts,
                                modifier = Modifier.alignBy(FirstBaseline)
                            )
                            Text(
                                text = review.completedToday.size.toString(),
                                modifier = Modifier.alignBy(FirstBaseline),
                                style = TextStyle(
                                    fontSize = 17.sp,
                                    fontFamily = fonts,
                                    color = colors.Body
                                )
                            )
                        }
                        if (review.completedToday.isEmpty()) {
                            Text(
                                text = "No checkmarks today — showing up still counts.",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    lineHeight = 21.sp,
                                    fontFamily = fonts,
                                    color = colors.NoteMuted
                                ),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    // ── Still open ─────────────────────────────────────────
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .padding(top = 34.dp)
                                .fillMaxWidth()
                                .nightReveal(revealAt(RV_OPEN_HEADER)),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SectionEyebrow(
                                text = "STILL OPEN",
                                fonts = fonts,
                                modifier = Modifier.alignBy(FirstBaseline)
                            )
                            Text(
                                text = openItems.size.toString(),
                                modifier = Modifier.alignBy(FirstBaseline),
                                style = TextStyle(
                                    fontSize = 17.sp,
                                    fontFamily = fonts,
                                    color = colors.Body
                                )
                            )
                        }

                        if (openItems.isEmpty()) {
                            Text(
                                text = "Nothing left open. Enjoy the evening.",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    lineHeight = 21.sp,
                                    fontFamily = fonts,
                                    color = colors.NoteMuted
                                ),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                            openItems.forEachIndexed { index, task ->
                                OpenTaskRow(
                                    task = task,
                                    fonts = fonts,
                                    reveal = revealAt(
                                        (RV_ITEM_BASE + RV_ITEM_STEP * index).coerceAtMost(RV_ITEM_MAX)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── Bottom bar ─────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .nightReveal(revealAt(RV_BAR))
                ) {
                    HorizontalDivider(color = colors.NoteBorder, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 30.dp, end = 30.dp, top = 14.dp, bottom = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(colors.Accent)
                                .clickable(enabled = !closing) { closing = true },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Done for today",
                                style = TextStyle(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = fonts,
                                    color = colors.OnAccent
                                )
                            )
                        }
                    }
                }
            }
        }

        // ── Closing state: "Good night" ────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = closingFade }
                .padding(horizontal = 30.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.Center)) {
                Text(
                    text = "Good night, Ovi.",
                    style = TextStyle(
                        fontSize = 40.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = fonts,
                        letterSpacing = (-0.4).sp,
                        color = colors.Heading
                    )
                )
                Text(
                    text = closingLine(review?.let { r -> (r.pending + r.pushedToTomorrow).size } ?: 0),
                    style = TextStyle(
                        fontSize = 19.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = fonts,
                        color = colors.Greeting
                    ),
                    modifier = Modifier.padding(top = 16.dp)
                )
                state.message?.let { message ->
                    Text(
                        text = message,
                        style = TextStyle(
                            fontSize = 15.sp,
                            lineHeight = 21.sp,
                            fontFamily = fonts,
                            color = colors.Muted
                        ),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 52.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(enabled = closing) { closing = false },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Back",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontFamily = fonts,
                        color = colors.Accent
                    )
                )
            }
        }
    }
}

@Composable
private fun SectionEyebrow(text: String, fonts: FontFamily, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = fonts,
            letterSpacing = 1.56.sp,
            color = CheckinFlowColors.Accent
        )
    )
}

@Composable
private fun OpenTaskRow(
    task: Task,
    fonts: FontFamily,
    reveal: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .nightReveal(reveal),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(14.dp)
                .border(1.5.dp, CheckinFlowColors.NoteMuted, CircleShape)
        )
        Column {
            Text(
                text = task.title,
                style = TextStyle(
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    fontFamily = fonts,
                    color = CheckinFlowColors.Body
                )
            )
            task.dueDate?.let { due ->
                val formatted = formatDueDate(due)
                if (formatted.isNotEmpty()) {
                    Text(
                        text = "due $formatted",
                        style = TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontFamily = fonts,
                            color = CheckinFlowColors.Muted
                        )
                    )
                }
            }
        }
    }
}

/** Reveal progress for one block: appears [delayMillis] after composition,
 *  then fades and rises 12px over 900ms ease-out (the design's .rv). */
@Composable
private fun revealAt(delayMillis: Long): Float {
    var shown by remember(delayMillis) { mutableStateOf(false) }
    LaunchedEffect(delayMillis) {
        delay(delayMillis)
        shown = true
    }
    return animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(900, easing = EaseOut),
        label = "nightReveal"
    ).value
}

/** Design's reveal: fade in while rising 12px, 900ms ease-out. */
private fun Modifier.nightReveal(progress: Float): Modifier = graphicsLayer {
    alpha = progress
    translationY = (1f - progress) * 12.dp.toPx()
}

/** Hero line; second half is lavender-soft in the design. */
@Composable
private fun heroText(completed: Int, highlight: Color): AnnotatedString =
    if (completed == 0) {
        buildAnnotatedString {
            append("Today you didn’t mark any tasks as complete, ")
            withStyle(SpanStyle(color = highlight)) {
                append("which is fine — sometimes a pause is needed.")
            }
        }
    } else {
        val noun = if (completed == 1) "task" else "tasks"
        buildAnnotatedString {
            append("Today you marked $completed $noun as complete. ")
            withStyle(SpanStyle(color = highlight)) {
                append("That’s a good day’s work.")
            }
        }
    }

private fun leadText(open: Int): String = when {
    open == 0 -> "Nothing is waiting for tomorrow — you closed out the day."
    open == 1 -> "1 item is waiting for tomorrow, ready when you are."
    else -> "$open items are waiting for tomorrow, ready when you are."
}

private fun closingLine(open: Int): String = when {
    open == 0 -> "Everything is closed out — rest well."
    open == 1 -> "Your 1 open item will be waiting for tomorrow."
    else -> "Your $open open items will be waiting for tomorrow."
}

private fun formatDate(isoDate: String): String = runCatching {
    LocalDate.parse(isoDate).format(reviewDateFormatter)
}.getOrDefault(isoDate)

private fun formatDueDate(millis: Long): String = runCatching {
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(dueDateFormatter)
}.getOrDefault("")
