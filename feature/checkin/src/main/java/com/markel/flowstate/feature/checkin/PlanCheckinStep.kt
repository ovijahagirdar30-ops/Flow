package com.markel.flowstate.feature.checkin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markel.flowstate.core.domain.EveningPlan
import com.markel.flowstate.core.domain.PlanBlock
import com.markel.flowstate.core.domain.PlanBlockKind
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Final check-in step, ported from the "Planning to Plan" design (frames
 * 3–8). Two stages crossfade within the step:
 *
 *  - Planning — aurora + "Planning your evening" while the planner runs
 *    (frames 2a–2d / 3), driven by [isPlanning] or a not-yet-generated plan.
 *  - Settled — "Your evening" header with the Edit pill, the date line and
 *    the timeline list that reveals one row at a time (frames 4–6):
 *    time column, hollow checkbox, connector line, title + meta. Edit mode
 *    (frame 8) swaps the pill to Done and adds per-row remove plus the
 *    "+ Add something" row.
 *
 * The verdict controls are kept from the legacy step so nothing loses
 * behaviour: Agree persists (bottom pill, the design's CTA), the optional
 * comment + Regenerate feeds the planner, Not tonight discards. [onDiscard]
 * and the edit callbacks keep their legacy contracts.
 */
@Composable
fun PlanCheckinStep(
    plan: EveningPlan?,
    isPlanning: Boolean,
    onRegenerate: (comment: String) -> Unit,
    onAgree: () -> Unit,
    onDiscard: () -> Unit,
    onEditBlock: (Int, PlanBlock) -> Unit,
    onRemoveBlock: (Int) -> Unit,
    onAddBlock: (PlanBlock) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = plan
    Crossfade(
        targetState = isPlanning || p == null,
        animationSpec = tween(650),
        label = "planStage"
    ) { planning ->
        if (planning || p == null) {
            CheckinPlanningStep(modifier = modifier)
        } else {
            SettledPlan(
                plan = p,
                onRegenerate = onRegenerate,
                onAgree = onAgree,
                onDiscard = onDiscard,
                onEditBlock = onEditBlock,
                onRemoveBlock = onRemoveBlock,
                onAddBlock = onAddBlock,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun SettledPlan(
    plan: EveningPlan,
    onRegenerate: (comment: String) -> Unit,
    onAgree: () -> Unit,
    onDiscard: () -> Unit,
    onEditBlock: (Int, PlanBlock) -> Unit,
    onRemoveBlock: (Int) -> Unit,
    onAddBlock: (PlanBlock) -> Unit,
    modifier: Modifier = Modifier
) {
    val fonts = rememberCheckinFonts()
    var comment by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableIntStateOf(-1) }
    var adding by remember { mutableStateOf(false) }

    // Progressive reveal (frames 5a→6): rows arrive one at a time, keyed on
    // the generation identity so later edits don't restart the sequence.
    val revealKey = plan.generatedAtMillis
    val initialCount = remember(revealKey) { plan.blocks.size }
    var revealed by remember(revealKey) { mutableIntStateOf(0) }
    LaunchedEffect(revealKey) {
        revealed = 0
        for (i in 1..initialCount) {
            delay(if (i == 1) 220L else 340L)
            revealed = i
        }
    }
    val visibleCount = if (revealed >= initialCount) plan.blocks.size else revealed

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CheckinFlowColors.Background)
    ) {
        // The settled frames keep a dimmer beam behind the header.
        CheckinAurora(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.62f),
            strength = 0.5f
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = 30.dp)
            ) {
                Spacer(modifier = Modifier.height(34.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Your evening",
                        style = TextStyle(
                            fontSize = 40.sp,
                            lineHeight = 46.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = fonts,
                            letterSpacing = (-0.4).sp,
                            color = CheckinFlowColors.Heading
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    EditPill(
                        label = if (editing) "Done" else "Edit",
                        onClick = { editing = !editing }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = planDateLabel(plan.date),
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = fonts,
                        color = CheckinFlowColors.Greeting
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Here's what tonight looks like.",
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontFamily = fonts,
                        color = CheckinFlowColors.Muted
                    )
                )

                if (plan.headline.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = plan.headline,
                        style = TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontFamily = fonts,
                            color = CheckinFlowColors.NoteMuted
                        )
                    )
                }

                // Fallback honesty: every network failure silently swaps in
                // the offline planner — say so instead of pretending it was AI.
                if (plan.offline) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Couldn't reach the AI — this is the offline plan. " +
                            "Tap Regenerate to try again.",
                        style = TextStyle(
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontFamily = fonts,
                            color = Color(0xFFF2B8B5)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(34.dp))

                plan.blocks.forEachIndexed { index, block ->
                    AnimatedVisibility(
                        visible = index < visibleCount,
                        enter = fadeIn(tween(380)) +
                            slideInVertically(tween(380)) { it / 4 }
                    ) {
                        PlanTimelineRow(
                            block = block,
                            isLast = index == plan.blocks.lastIndex,
                            editable = editing,
                            onClick = { editingIndex = index },
                            onRemove = { onRemoveBlock(index) },
                            fonts = fonts
                        )
                    }
                }

                if (plan.blocks.isEmpty() && !editing) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Nothing planned yet.",
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontFamily = fonts,
                            color = CheckinFlowColors.Muted
                        )
                    )
                }

                if (editing) {
                    AddSomethingRow(onClick = { adding = true }, fonts = fonts)
                    Spacer(modifier = Modifier.height(28.dp))
                } else {
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        placeholder = {
                            Text(
                                text = "What should change? (optional)",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontFamily = fonts,
                                    color = CheckinFlowColors.NoteMuted
                                )
                            )
                        },
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            fontFamily = fonts,
                            color = CheckinFlowColors.Body
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CheckinFlowColors.NoteBorder,
                            unfocusedBorderColor = CheckinFlowColors.NoteBorder,
                            cursorColor = CheckinFlowColors.Accent,
                            focusedContainerColor = Color(0xFF141019),
                            unfocusedContainerColor = Color(0xFF141019)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onRegenerate(comment)
                                comment = ""
                            },
                            border = BorderStroke(1.dp, CheckinFlowColors.NoteBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CheckinFlowColors.Accent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text(
                                text = "Regenerate",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontFamily = fonts
                                )
                            )
                        }

                        androidx.compose.material3.TextButton(
                            onClick = onDiscard,
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text(
                                text = "Not tonight",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontFamily = fonts,
                                    color = CheckinFlowColors.Muted
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Fixed bottom CTA — the design's pill.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
            ) {
                Button(
                    onClick = { if (editing) editing = false else onAgree() },
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CheckinFlowColors.Accent,
                        contentColor = CheckinFlowColors.OnAccent
                    ),
                    elevation = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = if (editing) "Save" else "Agree",
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = fonts
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    val editIndex = editingIndex
    if (editIndex in plan.blocks.indices) {
        PlanBlockEditorDialog(
            initial = plan.blocks[editIndex],
            onConfirm = {
                onEditBlock(editIndex, it)
                editingIndex = -1
            },
            onRemove = {
                onRemoveBlock(editIndex)
                editingIndex = -1
            },
            onDismiss = { editingIndex = -1 }
        )
    }

    if (adding) {
        PlanBlockEditorDialog(
            initial = null,
            onConfirm = {
                onAddBlock(it)
                adding = false
            },
            onRemove = null,
            onDismiss = { adding = false }
        )
    }
}

/** The bordered Edit/Done pill in the header row. */
@Composable
private fun EditPill(label: String, onClick: () -> Unit) {
    val fonts = rememberCheckinFonts()
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .border(1.dp, CheckinFlowColors.NoteBorder, RoundedCornerShape(20.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = fonts,
                color = CheckinFlowColors.Accent
            )
        )
    }
}

/**
 * One timeline row: right-aligned time, hollow checkbox with the connector
 * line running to the next row, then title + meta. Fixed height keeps the
 * connector segments aligned across rows inside the scrollable column.
 */
@Composable
private fun PlanTimelineRow(
    block: PlanBlock,
    isLast: Boolean,
    editable: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    fonts: FontFamily
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = editable,
                onClick = onClick
            ),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = formatPlanTime(block.startTime),
            modifier = Modifier
                .width(62.dp)
                .padding(top = 3.dp),
            textAlign = TextAlign.End,
            style = TextStyle(
                fontSize = 13.sp,
                fontFamily = fonts,
                color = CheckinFlowColors.Muted
            )
        )

        Column(
            modifier = Modifier
                .width(40.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(1.5.dp, CheckinFlowColors.NoteBorder, RoundedCornerShape(10.dp))
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(1.5.dp)
                        .background(CheckinFlowColors.Track)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = block.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontSize = 21.sp,
                    lineHeight = 26.sp,
                    fontFamily = fonts,
                    color = CheckinFlowColors.Heading
                )
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = blockMeta(block),
                style = TextStyle(
                    fontSize = 13.sp,
                    fontFamily = fonts,
                    color = CheckinFlowColors.Muted
                )
            )
        }

        if (editable) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onRemove
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Remove cross — no material-icons dependency in this flow.
                Canvas(modifier = Modifier.size(14.dp)) {
                    val stroke = 1.6.dp.toPx()
                    drawLine(
                        color = CheckinFlowColors.Accent,
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = CheckinFlowColors.Accent,
                        start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                        end = androidx.compose.ui.geometry.Offset(0f, size.height),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/** The "+ Add something" row shown at the end of the list in edit mode. */
@Composable
private fun AddSomethingRow(onClick: () -> Unit, fonts: FontFamily) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(1.5.dp, CheckinFlowColors.NoteBorder, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(11.dp)) {
                val stroke = 1.4.dp.toPx()
                drawLine(
                    color = CheckinFlowColors.Accent,
                    start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = CheckinFlowColors.Accent,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = "Add something",
            style = TextStyle(
                fontSize = 16.sp,
                fontFamily = fonts,
                color = CheckinFlowColors.Muted
            )
        )
    }
}

/** "2026-10-03" → "Saturday, October 3"; falls back to the raw date. */
private fun planDateLabel(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DATE_LABEL)
}.getOrDefault(iso)

private val DATE_LABEL: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())

/** The meta line under a title — design shows "30 min · …" or "fixed". */
private fun blockMeta(block: PlanBlock): String = when (block.kind) {
    PlanBlockKind.MEAL -> "fixed"
    else -> "${block.durationMinutes} min · ${block.kind.name.lowercase()}"
}
