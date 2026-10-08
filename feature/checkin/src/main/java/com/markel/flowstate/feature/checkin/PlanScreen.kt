package com.markel.flowstate.feature.checkin

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markel.flowstate.core.domain.EveningPlan
import com.markel.flowstate.core.domain.PlanBlock
import com.markel.flowstate.core.notifications.CheckinDebounce
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The Plan checklist tab, ported from the design's frame B ("Your evening"):
 * the page header (40px light title + date + Edit/Done text button), the
 * plan's headline, an "N of M done" line over a hairline progress bar, and
 * the timeline — time column, ring checkbox with connector and Now marker,
 * title/meta/note that dim once ticked — plus "+ Add something".
 *
 * Toggling Edit reveals the design's drag handles and the "Hold and drag to
 * move." hint: dragging a row swaps it between time SLOTS (its times stay
 * sorted; see [PlanBlockEdits.move]), and tapping a row opens the same block
 * editor as before. Ring taps keep the due-gate + overlap notices from
 * [PlanViewModel], rendered as the bottom overlay pill.
 *
 * Ticks are persisted by [PlanViewModel] (survive restarts); ticking a TASK
 * block marks the real task done, habit/free blocks tick visually only.
 * After the user's end-of-day time (or midnight) the plan ages out —
 * [PlanUiState.isExpired] — and the tab shows the same empty state as a
 * never-agreed evening. The app-owned bottom nav and Plan FAB stay on top;
 * the prototype's nav bar is not reproduced. Everything renders inside
 * FlowStateTheme via the shared check-in design seam, so it follows
 * light/dark and the user's chosen app color.
 */
@Composable
fun PlanScreen(
    viewModel: PlanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val plan = state.plan
    var isFabExpanded by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    // Re-read plan + ticks from Room on every resume — the FAB's "Edit plan"
    // persists through the check-in and hands back here, so the in-memory
    // snapshot from process start must never win over the database.
    LifecycleResumeEffect(Unit) {
        viewModel.reload()
        onPauseOrDispose { }
    }

    // Scaffold-equivalent wrapper (FlowScreen gets this from its Scaffold):
    // an opaque theme background so the page never flashes through.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> Unit

                    plan == null || state.isExpired -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            // Side gutters match the content, so the empty
                            // message never sits against the screen edge.
                            .padding(horizontal = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No plan yet — run the evening check-in and tap Agree to save one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    else -> PlanContent(
                        plan = plan,
                        checkedIndexes = state.checkedIndexes,
                        onToggle = viewModel::toggleBlock,
                        onEditBlock = viewModel::editBlock,
                        onRemoveBlock = viewModel::removeBlock,
                        onAddBlock = viewModel::addBlock,
                        onMoveBlock = viewModel::moveBlock,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Same expandable plus button as Tasks/Habits: manual check-in
            // fallback + plan-editing entry point.
            PlanFabMenu(
                expanded = isFabExpanded,
                onToggle = { isFabExpanded = !isFabExpanded },
                onEditPlanClick = {
                    isFabExpanded = false
                    startManualCheckin(context, startAtPlan = true)
                },
                onStartCheckinClick = {
                    isFabExpanded = false
                    startManualCheckin(context, startAtPlan = false)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .zIndex(1f)
            )

            // Transient page-overlay notice (too-early tick, overlap on a
            // drag, …) — the ViewModel auto-clears the state; this only
            // animates it in/out as a pill above the FAB.
            AnimatedVisibility(
                visible = state.message != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(2f)
                    .navigationBarsPadding()
                    .padding(start = 32.dp, end = 32.dp, bottom = 96.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = state.message.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Manual check-in entry behind the Plan tab's FAB. Marks today's once-per-day
 * guard as handled FIRST — so a later geofence arrival or 8AM weekend alarm
 * can't fire a second check-in today — then opens the check-in directly. The
 * action itself deliberately ignores that guard: it always opens, even if
 * today's check-in already happened. [startAtPlan] jumps straight to the
 * final plan step (the FAB's "Edit plan"), which falls back to the full flow
 * when no plan has been agreed yet.
 */
private fun startManualCheckin(context: Context, startAtPlan: Boolean) {
    CheckinDebounce.markCheckedInToday(context)
    val intent = Intent(context, CheckinActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        if (startAtPlan) {
            putExtra(CheckinActivity.EXTRA_START_STEP, CheckinActivity.START_STEP_PLAN)
        }
    }
    context.startActivity(intent)
}

@Composable
private fun PlanContent(
    plan: EveningPlan,
    checkedIndexes: Set<Int>,
    onToggle: (Int) -> Unit,
    // false = rejected (overlap) → the dialog stays open with the warning.
    onEditBlock: (Int, PlanBlock) -> Boolean,
    onRemoveBlock: (Int) -> Unit,
    onAddBlock: (PlanBlock) -> Boolean,
    // false = drag blocked (would double-book a time) → the drag ends and
    // the overlay pill explains why.
    onMoveBlock: (Int, Int) -> Boolean,
    modifier: Modifier = Modifier
) {
    val fonts = rememberCheckinFonts()
    val colors = CheckinFlowColors
    val total = plan.blocks.size
    val doneCount = checkedIndexes.count { it in plan.blocks.indices }
    // Design's rule: the first not-yet-ticked block is "now".
    val nowIndex = plan.blocks.indices.firstOrNull { it !in checkedIndexes } ?: -1

    var editMode by rememberSaveable { mutableStateOf(false) }
    // -1 sentinel = dialog closed; index = editing that block.
    var editingIndex by remember { mutableIntStateOf(-1) }
    var adding by remember { mutableStateOf(false) }
    // Overlap rejection: the dialog stays open with a red line inside it
    // (errorMessage), which then hides by itself after a couple of seconds.
    var dialogError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(dialogError) {
        if (dialogError != null) {
            delay(DIALOG_ERROR_MILLIS)
            dialogError = null
        }
    }

    // ── Drag-reorder state (edit mode) ───────────────────────────────────
    var dragIndex by remember { mutableIntStateOf(-1) }
    // Draw-phase only (read inside the dragged row's graphicsLayer), so
    // moving the finger never recomposes the list.
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    // Per-index row height in px, refreshed by onSizeChanged.
    val rowHeights = remember { mutableMapOf<Int, Int>() }
    // Snapshot of row heights at drag start, in block order; entries swap on
    // every crossing so each height keeps following its block. This is what
    // makes the visual offset continuous: the layout jumps by exactly the
    // neighbour's height, and the offset is reduced by the same amount.
    val dragHeights = remember { mutableListOf<Float>() }

    fun startDrag(index: Int) {
        if (dragIndex != -1) return
        dragIndex = index
        dragOffsetY = 0f
        dragHeights.clear()
        for (i in 0 until total) dragHeights.add((rowHeights[i] ?: 0).toFloat())
    }

    fun endDrag() {
        dragIndex = -1
        dragOffsetY = 0f
    }

    fun dragBy(delta: Float) {
        if (dragIndex !in 0 until total) return
        dragOffsetY += delta
        var guard = 0
        while (guard++ < total) {
            val i = dragIndex
            val down = dragHeights.getOrNull(i + 1)
            val up = dragHeights.getOrNull(i - 1)
            when {
                // Crossing into the row below: that row's height is the shift.
                down != null && down > 0f && dragOffsetY >= down -> {
                    if (!onMoveBlock(i, i + 1)) return endDrag()
                    dragHeights[i + 1] = dragHeights[i].also { dragHeights[i] = dragHeights[i + 1] }
                    dragOffsetY -= down
                    dragIndex = i + 1
                }
                // Crossing into the row above.
                up != null && up > 0f && dragOffsetY <= -up -> {
                    if (!onMoveBlock(i, i - 1)) return endDrag()
                    dragHeights[i - 1] = dragHeights[i].also { dragHeights[i] = dragHeights[i - 1] }
                    dragOffsetY += up
                    dragIndex = i - 1
                }
                else -> return
            }
        }
    }

    // Turning Edit off mid-drag drops the handle the gesture lives on.
    LaunchedEffect(editMode) { if (!editMode) endDrag() }

    val dateText = DateTimeFormatter
        .ofPattern("EEEE, d MMMM", LocalLocale.current.platformLocale)
        .format(LocalDate.now())

    val progress by animateFloatAsState(
        targetValue = if (total == 0) 0f else doneCount.toFloat() / total,
        animationSpec = tween(400),
        label = "planProgress"
    )

    Column(
        modifier = modifier
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 30.dp, end = 30.dp, top = 36.dp, bottom = 24.dp)
    ) {
        // ── Header: "Your evening" + date + Edit/Done ─────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "Your evening",
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
                    text = dateText,
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontFamily = fonts,
                        color = colors.Greeting
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Row(
                modifier = Modifier.height(44.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editMode) "Done" else "Edit",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = fonts,
                        color = colors.Accent
                    ),
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { editMode = !editMode }
                        .semantics { contentDescription = if (editMode) "Finish editing" else "Edit plan" }
                )
            }
        }

        Text(
            text = plan.headline,
            style = TextStyle(
                fontSize = 21.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Light,
                fontFamily = fonts,
                color = colors.Greeting
            ),
            modifier = Modifier.padding(top = 28.dp)
        )

        Text(
            text = "$doneCount of $total done",
            style = TextStyle(
                fontSize = 14.sp,
                fontFamily = fonts,
                color = colors.Muted
            ),
            modifier = Modifier.padding(top = 16.dp)
        )

        // ── Progress bar (hairline, 2dp) ──────────────────────────────────
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(colors.Track)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(colors.Accent)
            )
        }

        // ── Edit-mode hint (collapses when Edit turns off) ────────────────
        AnimatedVisibility(
            visible = editMode,
            enter = expandVertically(tween(300)) + fadeIn(tween(300)),
            exit = shrinkVertically(tween(300)) + fadeOut(tween(200))
        ) {
            Text(
                text = "Hold and drag to move.",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontFamily = fonts,
                    color = colors.NoteMuted
                ),
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        // ── Timeline ──────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(top = 36.dp)) {
            plan.blocks.forEachIndexed { index, block ->
                PlanTimelineRow(
                    block = block,
                    checked = index in checkedIndexes,
                    isNow = index == nowIndex,
                    editMode = editMode,
                    dragging = index == dragIndex,
                    onToggle = { onToggle(index) },
                    onOpenEdit = {
                        dialogError = null
                        editingIndex = index
                    },
                    onDragStart = { startDrag(index) },
                    onDragDelta = { dragBy(it) },
                    onDragEnd = { endDrag() },
                    onHeightMeasured = { height -> rowHeights[index] = height },
                    dragOffset = { dragOffsetY }
                )
            }
        }

        // ── "+ Add something" (design keeps it outside edit mode) ─────────
        Row(
            modifier = Modifier
                .padding(start = 70.dp, top = 4.dp)
                .height(44.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    dialogError = null
                    adding = true
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "+ Add something",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontFamily = fonts,
                    color = colors.Accent
                )
            )
        }
    }

    val editIndex = editingIndex
    if (editIndex in plan.blocks.indices) {
        PlanBlockEditorDialog(
            initial = plan.blocks[editIndex],
            onConfirm = {
                // false = overlap rejected: keep the dialog open with the
                // warning inside; true = applied, close it.
                if (onEditBlock(editIndex, it)) {
                    dialogError = null
                    editingIndex = -1
                } else {
                    dialogError = PlanBlockEdits.OVERLAP_MESSAGE
                }
            },
            onRemove = {
                onRemoveBlock(editIndex)
                dialogError = null
                editingIndex = -1
            },
            onDismiss = {
                dialogError = null
                editingIndex = -1
            },
            errorMessage = dialogError
        )
    }

    if (adding) {
        PlanBlockEditorDialog(
            initial = null,
            onConfirm = {
                if (onAddBlock(it)) {
                    dialogError = null
                    adding = false
                } else {
                    dialogError = PlanBlockEdits.OVERLAP_MESSAGE
                }
            },
            onRemove = null,
            onDismiss = {
                dialogError = null
                adding = false
            },
            errorMessage = dialogError
        )
    }
}

/**
 * One timeline row of frame B: 62px time column (with the "Now" marker),
 * 44px ring column (22px circle, -9dp pull-up, connector drawn behind down
 * to the row's bottom edge), the title/meta/note block that dims once
 * ticked, and — in edit mode — the drag handle that both starts a reorder
 * drag and opens the block editor on tap.
 */
@Composable
private fun PlanTimelineRow(
    block: PlanBlock,
    checked: Boolean,
    isNow: Boolean,
    editMode: Boolean,
    dragging: Boolean,
    onToggle: () -> Unit,
    onOpenEdit: () -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onHeightMeasured: (Int) -> Unit,
    dragOffset: () -> Float,
    modifier: Modifier = Modifier
) {
    val fonts = rememberCheckinFonts()
    // Hoisted for the draw scopes below — they are not composable.
    val colors = CheckinFlowColors
    val dim = colors.NoteMuted

    // Design's 300ms colour transitions on every text/ring state change.
    val timeColor by animateColorAsState(
        targetValue = if (checked) dim else if (isNow) colors.Accent else colors.Muted,
        animationSpec = tween(300), label = "rowTime"
    )
    val titleColor by animateColorAsState(
        targetValue = if (checked) dim else colors.Body,
        animationSpec = tween(300), label = "rowTitle"
    )
    val metaColor by animateColorAsState(
        targetValue = if (checked) dim else colors.Muted,
        animationSpec = tween(300), label = "rowMeta"
    )
    val noteColor by animateColorAsState(
        targetValue = if (checked) dim else colors.NoteMuted,
        animationSpec = tween(300), label = "rowNote"
    )
    val ringColor by animateColorAsState(
        targetValue = if (checked || isNow) colors.Accent else colors.NoteMuted,
        animationSpec = tween(300), label = "rowRing"
    )
    val ringFill by animateColorAsState(
        targetValue = if (checked) colors.Accent else Color.Transparent,
        animationSpec = tween(300), label = "rowFill"
    )
    val checkAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(300), label = "rowCheck"
    )
    val track = colors.Track
    // Centre of the ring column: 62dp time column + half of the 44dp column.
    val ringCenterX = 84.dp

    // Gesture callbacks reach into a pointerInput(Unit) block that survives
    // the index shifts a drag causes — read fresh copies every time.
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragDelta by rememberUpdatedState(onDragDelta)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { onHeightMeasured(it.height) }
            // The drag layer sits OUTSIDE drawBehind so the connector line
            // travels with the row while a drag translates it.
            .then(
                if (dragging) {
                    Modifier.graphicsLayer { translationY = dragOffset() }
                } else {
                    Modifier
                }
            )
            // Connector: from just under the ring to the row's bottom edge
            // (the content's padding-bottom carves out the design's gap).
            .drawBehind {
                val startY = 32.dp.toPx()
                if (startY < size.height) {
                    drawLine(
                        color = track,
                        start = Offset(ringCenterX.toPx(), startY),
                        end = Offset(ringCenterX.toPx(), size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            },
        verticalAlignment = Alignment.Top
    ) {
        // ── Time column ───────────────────────────────────────────────
        Column(modifier = Modifier.width(62.dp).padding(top = 4.dp)) {
            Text(
                text = formatPlanTime(block.startTime),
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = fonts,
                    color = timeColor
                )
            )
            if (isNow) {
                Text(
                    text = "Now",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = fonts,
                        color = colors.Accent
                    ),
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }

        // ── Ring column (the button pulls up 9dp, like the design's -9px) ─
        Box(
            modifier = Modifier
                .size(44.dp)
                .offset(y = (-9).dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggle
                )
                .semantics {
                    contentDescription =
                        (if (checked) "Mark not done: " else "Mark done: ") + block.title
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, ringColor, CircleShape)
                    .background(ringFill)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { alpha = checkAlpha }
                ) {
                    // The design's check: M6.5 11.5l3.2 3.2L16 8 on a 22 viewbox.
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(22.dp)) {
                        val path = Path().apply {
                            moveTo(6.5.dp.toPx(), 11.5.dp.toPx())
                            lineTo(9.7.dp.toPx(), 14.7.dp.toPx())
                            lineTo(16.dp.toPx(), 8.dp.toPx())
                        }
                        drawPath(
                            path = path,
                            color = colors.OnAccent,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }

        // ── Content ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp, bottom = 30.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = editMode,
                    onClick = onOpenEdit
                )
        ) {
            Text(
                text = block.title,
                style = TextStyle(
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontFamily = fonts,
                    color = titleColor
                )
            )
            Text(
                text = "${block.kind.name.lowercase().replaceFirstChar { it.uppercase() }} · ${block.durationMinutes} min",
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontFamily = fonts,
                    color = metaColor
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
            if (block.reason.isNotBlank()) {
                Text(
                    text = block.reason,
                    style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontFamily = fonts,
                        color = noteColor
                    ),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        // ── Drag handle (edit mode): drag to reorder, tap to edit ──────
        if (editMode) {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(44.dp)
                    .padding(top = 2.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { currentOnDragStart() },
                            onDragEnd = { currentOnDragEnd() },
                            onDragCancel = { currentOnDragEnd() },
                            onDrag = { change, amount ->
                                change.consume()
                                currentOnDragDelta(amount.y)
                            }
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenEdit
                    )
                    .semantics { contentDescription = "Reorder ${block.title}" },
                contentAlignment = Alignment.TopCenter
            ) {
                // The design's handle: three 14-wide lines, stroke #8A7A94.
                androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp).padding(top = 2.dp)) {
                    val stroke = 1.6.dp.toPx()
                    listOf(6f, 10f, 14f).forEach { y ->
                        drawLine(
                            color = colors.NoteMuted,
                            start = Offset(3.dp.toPx(), y.dp.toPx()),
                            end = Offset(17.dp.toPx(), y.dp.toPx()),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

/** How long the in-dialog overlap warning stays visible before hiding itself. */
private const val DIALOG_ERROR_MILLIS = 2_500L
