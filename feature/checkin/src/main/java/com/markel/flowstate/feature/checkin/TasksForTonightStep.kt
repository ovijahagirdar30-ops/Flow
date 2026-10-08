package com.markel.flowstate.feature.checkin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markel.flowstate.core.domain.CheckinItem
import com.markel.flowstate.core.domain.CheckinItemType

/**
 * "Tasks for tonight" — the hand-off step between the recap and the plan,
 * ported from the design's frame A. Adds a task for tonight (same shared
 * use case the Tasks screen reads from), shows tonight's TASKS with a remove
 * cross and the due-today HABITS read-only, then hands off with the
 * "Show my plan" pill.
 *
 * Like the PLAN step it owns its own chrome — no designed header/footer —
 * so it sits outside [CheckinStep.isDesignedFlow].
 */
@Composable
internal fun TasksForTonightStep(
    items: List<CheckinItem>,
    onAddTask: (String) -> Unit,
    onRemoveTask: (Int) -> Unit,
    onShowPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fonts = rememberCheckinFonts()
    val colors = CheckinFlowColors
    val tasks = remember(items) { items.filter { it.type == CheckinItemType.TASK } }
    val habits = remember(items) { items.filter { it.type == CheckinItemType.HABIT } }
    var draft by remember { mutableStateOf("") }
    val canAdd = draft.isNotBlank()

    fun submit() {
        val title = draft.trim()
        if (title.isEmpty()) return
        onAddTask(title)
        draft = ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.Background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 30.dp)
        ) {
            Spacer(modifier = Modifier.height(44.dp))

            Text(
                text = "What needs doing tonight?",
                style = TextStyle(
                    fontSize = 34.sp,
                    lineHeight = 39.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = fonts,
                    letterSpacing = (-0.34).sp,
                    color = colors.Heading
                )
            )

            Text(
                text = "Tasks and habits I'll fit into your evening.",
                style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontFamily = fonts,
                    color = colors.Muted
                ),
                modifier = Modifier.padding(top = 12.dp)
            )

            // ── Add a task ─────────────────────────────────────────────
            Column(modifier = Modifier.padding(top = 36.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (draft.isEmpty()) {
                            Text(
                                text = "Add a task for tonight…",
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontFamily = fonts,
                                    color = CheckinFlowColors.NoteMuted
                                )
                            )
                        }
                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 18.sp,
                                fontFamily = fonts,
                                color = colors.Body
                            ),
                            cursorBrush = SolidColor(colors.Accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "New task" }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .height(44.dp)
                            .padding(start = 12.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = canAdd
                            ) { submit() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = fonts,
                                color = if (canAdd) colors.Accent else colors.Accent.copy(alpha = 0.45f)
                            )
                        )
                    }
                }
                HorizontalDivider(color = colors.Accent.copy(alpha = 0.35f), thickness = 1.dp)
            }

            // ── Tasks ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.padding(top = 40.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionLabel(
                    text = "TASKS",
                    modifier = Modifier.alignBy(FirstBaseline)
                )
                Text(
                    text = tasks.size.toString(),
                    modifier = Modifier.alignBy(FirstBaseline),
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = fonts,
                        color = colors.Muted
                    )
                )
            }
            Column(modifier = Modifier.padding(top = 6.dp).fillMaxWidth()) {
                if (tasks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Nothing added yet.",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontFamily = fonts,
                                color = colors.Muted
                            )
                        )
                    }
                } else {
                    tasks.forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = task.title,
                                style = TextStyle(
                                    fontSize = 19.sp,
                                    lineHeight = 25.sp,
                                    fontFamily = fonts,
                                    color = colors.Body
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            RemoveButton(
                                title = task.title,
                                onClick = { onRemoveTask(task.sourceId) }
                            )
                        }
                        HorizontalDivider(color = colors.NoteBorder, thickness = 1.dp)
                    }
                }
            }

            // ── Habits (due today, read-only) ──────────────────────────
            if (habits.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 40.dp).fillMaxWidth()
                ) {
                    SectionLabel(text = "HABITS")
                }
                Column(modifier = Modifier.padding(top = 6.dp).fillMaxWidth()) {
                    habits.forEach { habit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LoopIcon()
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = habit.title,
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontFamily = fonts,
                                    color = colors.Body.copy(alpha = 0.87f)
                                )
                            )
                        }
                        HorizontalDivider(color = colors.NoteBorder, thickness = 1.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Bottom bar ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.Background)
                .navigationBarsPadding()
        ) {
            HorizontalDivider(color = colors.NoteBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.Accent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onShowPlan() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Show my plan",
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = fonts,
                        color = colors.OnAccent
                    )
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/** The design's 13px uppercase lavender section label. */
@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = rememberCheckinFonts(),
            letterSpacing = 1.56.sp,
            color = CheckinFlowColors.Accent
        )
    )
}

/** 44dp remove target with the design's 18px cross (stroke #8A7A94). */
@Composable
private fun RemoveButton(title: String, onClick: () -> Unit) {
    // Hoisted for the Canvas below — draw scopes are not composable.
    val colors = CheckinFlowColors
    Row(
        modifier = Modifier
            .size(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .semantics { contentDescription = "Remove $title" },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val stroke = 1.8.dp.toPx() * (18f / 24f)
            val path = Path().apply {
                moveTo(size.width * 6f / 24f, size.height * 6f / 24f)
                lineTo(size.width * 18f / 24f, size.height * 18f / 24f)
                moveTo(size.width * 18f / 24f, size.height * 6f / 24f)
                lineTo(size.width * 6f / 24f, size.height * 18f / 24f)
            }
            drawPath(
                path = path,
                color = colors.NoteMuted,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/** The design's 16px "loop" glyph marking each read-only habit row. */
@Composable
private fun LoopIcon() {
    // Hoisted for the Canvas below — draw scopes are not composable.
    val colors = CheckinFlowColors
    Canvas(modifier = Modifier.size(16.dp)) {
        val stroke = 1.8.dp.toPx() * (16f / 24f)
        val w = size.width
        val h = size.height
        fun x(v: Float) = w * v / 24f
        fun y(v: Float) = h * v / 24f

        val path = Path().apply {
            // Top stroke: up the left side, around the corner, across to the arrow.
            moveTo(x(4f), y(11f))
            lineTo(x(4f), y(9f))
            quadraticBezierTo(x(4f), y(5f), x(8f), y(5f))
            lineTo(x(20f), y(5f))
            // Top arrow head.
            moveTo(x(17f), y(2f))
            lineTo(x(20f), y(5f))
            lineTo(x(17f), y(8f))
            // Bottom stroke: down the right side, around the corner, back left.
            moveTo(x(20f), y(13f))
            lineTo(x(20f), y(15f))
            quadraticBezierTo(x(20f), y(19f), x(16f), y(19f))
            lineTo(x(4f), y(19f))
            // Bottom arrow head.
            moveTo(x(7f), y(22f))
            lineTo(x(4f), y(19f))
            lineTo(x(7f), y(16f))
        }
        drawPath(
            path = path,
            color = colors.NoteMuted,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
