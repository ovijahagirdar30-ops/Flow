package com.markel.flowstate.feature.habits.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markel.flowstate.core.domain.HabitSchedule
import com.markel.flowstate.core.domain.HabitWithStatus
import com.markel.flowstate.feature.habits.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

/**
 * The list card for every habit — same skeleton as the measurable card:
 * check button, name + goal line, ⋯ menu, week bar strip, then a controls
 * row. Boolean habits show the day's completion in the controls row where
 * the measurable card shows its value and +/− steppers.
 */
@Composable
fun HabitCard(
    habitWithStatus: HabitWithStatus,
    weekEntries: Set<Long>,
    onToggleDay: (LocalDate) -> Unit,
    onDelete: () -> Unit,
    habitCount: Int,
    onEdit: (name: String, icon: String, colorArgb: Int, priorityRank: Int, rolloverIfMissed: Boolean, moodLoggingEnabled: Boolean, schedule: HabitSchedule, reminderEnabled: Boolean, reminderMinuteOfDay: Int?) -> Unit,
    onNavigateToDetail: (() -> Unit)? = null
) {
    val habit = habitWithStatus.habit
    val habitColor = Color(habit.colorArgb)
    val today = LocalDate.now()
    val isCompletedToday = today.toEpochDay() in weekEntries
    val locale = LocalLocale.current.platformLocale

    // Week navigation — the same weekOffset model the measurable card uses.
    var weekOffset by remember { mutableIntStateOf(0) }
    val weekStart = remember(weekOffset) {
        today.with(DayOfWeek.MONDAY).plusWeeks(weekOffset.toLong())
    }
    val creationWeekStart = remember(habit.createdAt) {
        habit.createdAt.with(DayOfWeek.MONDAY)
    }
    val canGoBack = weekStart > creationWeekStart
    val canGoForward = weekOffset < 0

    var selectedDate by remember(weekOffset) {
        mutableStateOf(if (weekOffset == 0) today else weekStart)
    }

    // Goal line: the schedule, mirroring "Goal: … per day" on numeric cards.
    val dailyLabel = stringResource(R.string.habit_schedule_daily)
    val timesPerWeekLabel = habit.schedule.weeklyTarget?.let {
        stringResource(R.string.habit_schedule_times_per_week, it)
    }
    val scheduleText = remember(habit.schedule, dailyLabel, timesPerWeekLabel) {
        val days = habit.schedule.days
        if (days.size == 7 && habit.schedule.weeklyTarget == null) {
            dailyLabel
        } else {
            val dayList = days
                .sortedBy { it.value }
                .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }
            timesPerWeekLabel?.let { "$dayList · $it" } ?: dayList
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
    val cardBg by animateColorAsState(
        targetValue = if (isCompletedToday)
            habitColor.copy(alpha = 0.2f).compositeOver(surfaceColor)
        else
            surfaceColor,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "card_bg"
    )

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog   by remember { mutableStateOf(false) }
    var menuExpanded     by remember { mutableStateOf(false) }
    var swipeAccumulator by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 60f

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_habit_dialog_title)) },
            text = { Text(stringResource(R.string.delete_habit_dialog_message, habit.name)) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text(stringResource(R.string.delete_habit_confirm_button), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.delete_habit_cancel_button)) }
            }
        )
    }

    // ── Edit sheet (reuses AddHabitSheet in edit mode) ──────────────────────
    if (showEditDialog) {
        AddHabitSheet(
            initialName = habit.name,
            initialIcon = habit.iconName,
            initialColor = habitColor,
            initialPriorityRank = habit.priorityRank,
            initialRolloverIfMissed = habit.rolloverIfMissed,
            initialMoodLoggingEnabled = habit.moodLoggingEnabled,
            initialSchedule = habit.schedule,
            initialReminderEnabled = habit.reminderEnabled,
            initialReminderMinuteOfDay = habit.reminderMinuteOfDay,
            habitCount = habitCount,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, icon, colorArgb, priorityRank, rolloverIfMissed, moodLoggingEnabled, schedule, reminderEnabled, reminderMinuteOfDay ->
                onEdit(name, icon, colorArgb, priorityRank, rolloverIfMissed, moodLoggingEnabled, schedule, reminderEnabled, reminderMinuteOfDay)
                showEditDialog = false
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Top row ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = onNavigateToDetail != null,
                        onClick = { onNavigateToDetail?.invoke() }
                    )
                    .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MorphingCheckButton(
                    isCompleted = isCompletedToday,
                    color = habitColor,
                    iconName = habit.iconName,
                    onClick = { onToggleDay(today) }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = scheduleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Text(
                            "···",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 18.sp
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.habit_menu_edit)) },
                            onClick = {
                                menuExpanded  = false
                                showEditDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.delete_habit_confirm_button),)
                            },
                            onClick = {
                                menuExpanded   = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }

            // ── Week Bar graphic (same strip as the measurable card) ────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .pointerInput(canGoBack, canGoForward) {
                        detectHorizontalDragGestures(
                            onDragStart = { swipeAccumulator = 0f },
                            onDragEnd = { swipeAccumulator = 0f },
                            onDragCancel = { swipeAccumulator = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                swipeAccumulator += dragAmount
                                when {
                                    swipeAccumulator < -swipeThreshold && canGoForward -> {
                                        weekOffset++
                                        swipeAccumulator = 0f
                                    }
                                    swipeAccumulator > swipeThreshold && canGoBack -> {
                                        weekOffset--
                                        swipeAccumulator = 0f
                                    }
                                }
                            }
                        )
                    },
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                (0L..6L).forEach { offset ->
                    val date = weekStart.plusDays(offset)
                    val isFuture = date.isAfter(today)
                    val isDone = date.toEpochDay() in weekEntries

                    NumericWeekBar(
                        value = when {
                            isFuture -> null
                            isDone -> 1f
                            else -> 0f
                        },
                        targetValue = 1f,
                        scaleReference = 1f,
                        color = habitColor,
                        date = date,
                        isToday = date == today,
                        isFuture = isFuture,
                        isSelected = date == selectedDate,
                        onClick = {
                            selectedDate = date
                            onToggleDay(date)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Controls ────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                val selectedDone = !selectedDate.isAfter(today) &&
                        selectedDate.toEpochDay() in weekEntries
                Surface(
                    onClick = { onToggleDay(selectedDate) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .height(42.dp)
                        .width(120.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (selectedDone)
                                "✓ ${stringResource(R.string.habit_monthly_completed)}"
                            else
                                "○ ${stringResource(R.string.habit_card_missed)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedDone) habitColor
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
