package com.markel.flowstate.feature.habits.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markel.flowstate.core.domain.HabitWithStatus
import com.markel.flowstate.feature.habits.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.times

@Composable
fun NumericHabitCard(
    habitWithStatus: HabitWithStatus,
    onIncrementToday: () -> Unit,
    onDecrementToday: () -> Unit,
    onSetValue: (LocalDate, Float?) -> Unit,
    onDelete: () -> Unit,
    onEdit: (name: String, icon: String, colorArgb: Int, unit: String?, targetValue: Float?, step: Float?) -> Unit,
    onNavigateToDetail: (() -> Unit)? = null
) {
    val habit = habitWithStatus.habit
    val habitColor = Color(habit.colorArgb)
    val today = LocalDate.now()
    val weekStart = today.with(DayOfWeek.MONDAY)
    var selectedDate by remember { mutableStateOf(today) }

    // Get the value of the selected date
    val selectedDayIndex = selectedDate.dayOfWeek.value - 1
    val selectedValue = if (selectedDate.isAfter(today)) {
        null
    } else {
        habitWithStatus.weekValues.getOrNull(selectedDayIndex)
    } ?: 0f

    val targetValue = habit.targetValue
    val isCompletedToday = habitWithStatus.isCompletedToday
    
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
    var showEditDialog by remember { mutableStateOf(false) }
    var showInputDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Delete dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_habit_dialog_title)) },
            text = { Text(stringResource(R.string.delete_habit_dialog_message, habit.name)) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text(
                        stringResource(R.string.delete_habit_confirm_button),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.delete_habit_cancel_button))
                }
            }
        )
    }

    // Edition dialog
    if (showEditDialog) {
        AddHabitDialog(
            initialName = habit.name,
            initialIcon = habit.iconName,
            initialColor = habitColor,
            initialHabitType = habit.habitType,
            initialUnit = habit.unit,
            initialTargetValue = habit.targetValue,
            initialStep = habit.step,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, icon, colorArgb, _, unit, target, step ->
                onEdit(name, icon, colorArgb, unit, target, step)
                showEditDialog = false
            }
        )
    }

    // Input dialog
    if (showInputDialog) {
        val currentValueForDialog = habitWithStatus.weekValues.getOrNull(selectedDayIndex)
        NumericInputDialog(
            habitName = habit.name,
            unit = habit.unit,
            currentValue = currentValueForDialog,
            onDismiss = { showInputDialog = false },
            onConfirm = { value ->
                onSetValue(selectedDate, value)
                showInputDialog = false
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
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
                    onClick = { } // Nothing to do here yet, only used as an indicator
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (targetValue != null) {
                        Text(
                            text = stringResource(
                                R.string.habit_target_preview,
                                if (targetValue % 1 == 0f) targetValue.toInt().toString() else targetValue.toString(),
                                habit.unit ?: ""
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                                menuExpanded = false
                                showEditDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.delete_habit_confirm_button))
                            },
                            onClick = {
                                menuExpanded = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }

            // ── Week Bar graphic ──────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Calculate the max value for reference
                val maxWeekValue = habitWithStatus.weekValues.filterNotNull().maxOrNull() ?: 1f
                val scaleReference = when {
                    targetValue != null -> maxOf(targetValue, maxWeekValue)
                    else -> maxWeekValue
                }

                habitWithStatus.weekValues.forEachIndexed { index, value ->
                    val date = weekStart.plusDays(index.toLong())
                    val isFuture = date.isAfter(today)
                    val isToday = date == today
                    val isSelected = date == selectedDate

                    NumericWeekBar(
                        value = if (isFuture) null else value,
                        targetValue = targetValue,
                        scaleReference = scaleReference,
                        color = habitColor,
                        dayOfWeek = DayOfWeek.of(index + 1),
                        isToday = isToday,
                        isFuture = isFuture,
                        isSelected = isSelected,
                        onClick = {
                            if (!isFuture) {
                                selectedDate = date
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Controls ──────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                // Actual value (clickable to edit)
                Surface(
                    onClick = { showInputDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .height(42.dp)
                        .width(120.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (selectedValue % 1 == 0f) {
                                selectedValue.toInt().toString()
                            } else {
                                selectedValue.toString()
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (habit.unit != null) {
                            Text(
                                text = " ${habit.unit}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.edit_24px),
                            contentDescription = "Edit value",
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val decInteraction = remember { MutableInteractionSource() }
                    val isDecPressed by decInteraction.collectIsPressedAsState()
                    val decScale by animateFloatAsState(
                        targetValue = if (isDecPressed) 0.9f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 1200f)
                    )

                    // Decrement button
                    FilledTonalIconButton(
                        onClick = {
                            if (selectedDate == today) {
                                onDecrementToday()
                            } else {
                                val currentVal =
                                    habitWithStatus.weekValues.getOrNull(selectedDayIndex) ?: 0f
                                val newVal = maxOf(0f, currentVal - habit.step)
                                onSetValue(selectedDate, if (newVal > 0f) newVal else null)
                            }
                        },
                        interactionSource = decInteraction,
                        enabled = selectedValue > 0,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = habitColor.copy(alpha = 0.60f),
                            contentColor = habitColor
                        ),
                        modifier = Modifier
                            .size(width = 46.dp, height = 42.dp)
                            .graphicsLayer(scaleX = decScale, scaleY = decScale),
                        shape = RoundedCornerShape(
                            topStart = CornerSize(50),
                            bottomStart = CornerSize(50),
                            topEnd = CornerSize(8.dp),
                            bottomEnd = CornerSize(8.dp)
                        )
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.remove_24px),
                            contentDescription = "Decrement",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val incInteraction = remember { MutableInteractionSource() }
                    val isIncPressed by incInteraction.collectIsPressedAsState()
                    val incScale by animateFloatAsState(
                        targetValue = if (isIncPressed) 0.9f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 1200f)
                    )
                    // Increment button
                    FilledTonalIconButton(
                        onClick = {
                            if (selectedDate == today) {
                                onIncrementToday()
                            } else {
                                val currentVal =
                                    habitWithStatus.weekValues.getOrNull(selectedDayIndex) ?: 0f
                                val newVal = currentVal + habit.step
                                onSetValue(selectedDate, newVal)
                            }
                        },
                        interactionSource = incInteraction,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = habitColor.copy(alpha = 0.60f),
                            contentColor = habitColor
                        ),
                        modifier = Modifier
                            .size(width = 46.dp, height = 42.dp)
                            .graphicsLayer(scaleX = incScale, scaleY = incScale),
                        shape = RoundedCornerShape(
                            topStart = CornerSize(8.dp),
                            bottomStart = CornerSize(8.dp),
                            topEnd = CornerSize(50),
                            bottomEnd = CornerSize(50)
                        )
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.add_24px),
                            contentDescription = "Increment",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

        }
    }
}