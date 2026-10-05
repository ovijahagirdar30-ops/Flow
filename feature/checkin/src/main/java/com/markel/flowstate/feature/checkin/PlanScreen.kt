package com.markel.flowstate.feature.checkin

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markel.flowstate.core.domain.EveningPlan
import com.markel.flowstate.core.domain.PlanBlock
import com.markel.flowstate.core.notifications.CheckinDebounce
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The Plan checklist tab: tonight's most recently AGREED evening plan with a
 * checkbox per block, plus the headline, date, and a done counter. Ticks are
 * persisted by [PlanViewModel] (survive restarts); ticking a TASK block marks
 * the real task done, habit/free blocks tick visually only.
 *
 * After the user's end-of-day time (or at midnight, the default cutoff) the
 * plan ages out — [PlanUiState.isExpired] — and the tab shows the same empty
 * state as a never-agreed evening, so yesterday's plan never lingers.
 *
 * Opens with [PlanHeader] — the Flow tab's header recipe (bold italic
 * headlineLarge + uppercase primary date, 24dp gutters, greeting collapses on
 * first scroll) so the two tabs read as one app. Everything renders inside
 * the main app's FlowStateTheme, so it follows light/dark and the user's
 * chosen app color.
 */
@Composable
fun PlanScreen(
    viewModel: PlanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val plan = state.plan
    // Collapses the header's title line on first scroll, exactly like the
    // Flow tab's isHeaderMinimized saveable state.
    var isHeaderMinimized by rememberSaveable { mutableStateOf(false) }
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
    // an opaque theme background that ALSO supplies LocalContentColor —
    // onBackground. Without a Surface the header title inherits Compose's
    // default content colour, black, and vanishes into the dark background.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                PlanHeader(isMinimized = isHeaderMinimized)

                when {
                    state.isLoading -> Unit

                    plan == null || state.isExpired -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // Side gutters match the header, so the empty message
                            // never sits against the screen edge.
                            .padding(horizontal = 24.dp),
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
                        onScrolled = { isHeaderMinimized = true },
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

/**
 * Page header mirroring [com.markel.flowstate.feature.flow.components.DynamicHeader]
 * (kept local — feature modules don't depend on each other): status-bar aware,
 * 24dp gutters, a 65→35dp animated box, the bold italic headlineLarge title
 * that fades/shrinks away when minimized, and the uppercase primary date line.
 */
@Composable
private fun PlanHeader(isMinimized: Boolean) {
    val dateText = DateTimeFormatter
        .ofPattern("EEEE, d MMM", LocalLocale.current.platformLocale)
        .format(LocalDate.now())
        .uppercase()

    val headerHeight by animateDpAsState(
        targetValue = if (isMinimized) 35.dp else 65.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "height"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .animateContentSize(),
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = !isMinimized,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        text = "Today's Plans",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            fontStyle = FontStyle.Italic
                        )
                    )
                }
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.animateContentSize()
                )
            }
        }
    }
}

@Composable
private fun PlanContent(
    plan: EveningPlan,
    checkedIndexes: Set<Int>,
    onToggle: (Int) -> Unit,
    onEditBlock: (Int, PlanBlock) -> Unit,
    onRemoveBlock: (Int) -> Unit,
    onAddBlock: (PlanBlock) -> Unit,
    onScrolled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val doneCount = checkedIndexes.count { it in plan.blocks.indices }
    // -1 sentinel = dialog closed; index = editing that block.
    var editingIndex by remember { mutableIntStateOf(-1) }
    var adding by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    // Collapse the header's title line the first time the content moves —
    // the Flow tab does the same from its LazyList onScrolled callback.
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value > 0 }.collect { moved ->
            if (moved) onScrolled()
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = plan.headline,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "$doneCount of ${plan.blocks.size} done",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        plan.blocks.forEachIndexed { index, block ->
            PlanBlockRow(
                block = block,
                checked = index in checkedIndexes,
                onToggle = { onToggle(index) },
                onEdit = { editingIndex = index }
            )
            HorizontalDivider()
        }

        OutlinedButton(
            onClick = { adding = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add block")
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

@Composable
private fun PlanBlockRow(
    block: PlanBlock,
    checked: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 12.dp, bottom = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatPlanTime(block.startTime),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${block.kind.name.lowercase().replaceFirstChar { it.uppercase() }} · ${block.durationMinutes} min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = block.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textDecoration = if (checked) TextDecoration.LineThrough else null
            )
            Text(
                text = block.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TextButton(onClick = onEdit) {
            Text("Edit", style = MaterialTheme.typography.labelMedium)
        }
    }
}
