package com.markel.flowstate.feature.habits.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.markel.flowstate.core.domain.HabitType
import com.markel.flowstate.feature.habits.R

private val habitColors = listOf(
    Color(0xFF6650A4), Color(0xFF0061A4), Color(0xFF006E1C),
    Color(0xFFB3261E), Color(0xFFE8710A), Color(0xFF006A6A),
    Color(0xFF6B5778), Color(0xFF386666)
)

@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        icon: String,
        colorArgb: Int,
        habitType: HabitType,
        unit: String?,
        targetValue: Float?,
        step: Float
    ) -> Unit,
    initialName: String = "",
    initialIcon: String = "none",
    initialColor: Color? = null,
    initialHabitType: HabitType = HabitType.BOOLEAN,
    initialUnit: String? = null,
    initialTargetValue: Float? = null,
    initialStep: Float = 1f
) {
    val isEditMode = initialName.isNotEmpty() || initialColor != null
    var name by remember { mutableStateOf(initialName) }
    var selectedIcon by remember { mutableStateOf(initialIcon) }
    var selectedColor by remember { mutableStateOf( initialColor?.let { ic -> habitColors.firstOrNull { it == ic } } ?: habitColors.first()) }
    var habitType by remember { mutableStateOf(initialHabitType) }
    var unit by remember { mutableStateOf(initialUnit ?: "") }
    var targetValueText by remember { mutableStateOf(initialTargetValue?.toString() ?: "") }
    var stepText by remember { mutableStateOf(initialStep.toString())}

    val parsedTarget = targetValueText.toFloatOrNull()
    val parsedStep = stepText.toFloatOrNull()
    val isTargetInvalid = habitType == HabitType.NUMERIC && targetValueText.isNotBlank() && (parsedTarget == null || parsedTarget <= 0f)
    val isStepInvalid = habitType == HabitType.NUMERIC && stepText.isNotBlank() && (parsedStep == null || parsedStep <= 0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(
            stringResource(
                if (isEditMode) R.string.edit_habit_dialog_title
                else R.string.add_habit_dialog_title
            )
        ) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.add_habit_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Habit type selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.habit_type),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = habitType == HabitType.BOOLEAN,
                            onClick = { habitType = HabitType.BOOLEAN },
                            label = { Text(
                                text = stringResource(R.string.habit_type_boolean),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            ) },
                            modifier = Modifier.weight(1f),
                            enabled = !isEditMode
                        )
                        FilterChip(
                            selected = habitType == HabitType.NUMERIC,
                            onClick = { habitType = HabitType.NUMERIC },
                            label = { Text(
                                text = stringResource(R.string.habit_type_numeric),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            ) },
                            modifier = Modifier.weight(1f),
                            enabled = !isEditMode
                        )
                    }
                }

                // Additional fields for numeric habits
                AnimatedVisibility(
                    visible = habitType == HabitType.NUMERIC,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        // Unit
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text(stringResource(R.string.habit_unit_label)) },
                            placeholder = { Text(
                                text = " h, km, kg...",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                ) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = { Text("") }  // Added to make the padding between all text fields the same
                        )

                        // Goal
                        OutlinedTextField(
                            value = targetValueText,
                            onValueChange = { targetValueText = it },
                            label = {
                                Text("${stringResource(R.string.habit_target_label)} (${stringResource(R.string.habit_target_optional)})")
                            },
                            placeholder = { Text(
                                text = "2",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            ) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            isError = isTargetInvalid,
                            supportingText = {
                                when {
                                    isTargetInvalid -> {
                                        Text(text = stringResource(R.string.habit_target_error))
                                    }
                                    unit.isNotBlank() && targetValueText.isNotBlank() -> {
                                        Text(stringResource(R.string.habit_target_preview, targetValueText, unit))
                                    }
                                }
                            }
                        )

                        // Step
                        OutlinedTextField(
                            value = stepText,
                            onValueChange = { stepText = it },
                            label = { Text(stringResource(R.string.habit_step_label)) },
                            placeholder = { Text("1, 0.5 ...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            isError = isStepInvalid,
                            supportingText = {
                                if (isStepInvalid) {
                                    Text(text = stringResource(R.string.habit_step_error))
                                } else {
                                    Text(stringResource(R.string.habit_step_explanation))
                                }
                            }
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.icon), style = MaterialTheme.typography.labelLarge)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(HabitIconList) { (iconName, vector) ->
                            val isSelected = iconName == selectedIcon
                            val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                            IconButton(
                                onClick = { selectedIcon = iconName },
                                modifier = Modifier.background(containerColor, CircleShape)
                            ) {
                                if (vector != null) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(vector),
                                        contentDescription = iconName,
                                        tint = contentColor
                                    )
                                }
                                else {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.block_24px),
                                        contentDescription = "No icon",
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.add_habit_color_label),
                        style = MaterialTheme.typography.labelMedium
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(75.dp)
                    ) {
                        items(habitColors) { color ->
                            val isSelected = color == selectedColor
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (isSelected) Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            CircleShape
                                        ) else Modifier
                                    )
                                    .clickable { selectedColor = color }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedTarget = targetValueText.toFloatOrNull()
                    val parsedStep = stepText.toFloatOrNull() ?: 1f

                    onConfirm(
                        name,
                        selectedIcon,
                        selectedColor.toArgb(),
                        habitType,
                        if (habitType == HabitType.NUMERIC && unit.isNotBlank()) unit else null,
                        if (habitType == HabitType.NUMERIC) parsedTarget else null,
                        if (habitType == HabitType.NUMERIC && stepText.isNotBlank()) parsedStep else 1f
                    )
                },
                enabled = name.isNotBlank() && (habitType == HabitType.BOOLEAN || habitType == HabitType.NUMERIC) && (!isTargetInvalid && !isStepInvalid)
            ) { Text(
                stringResource(
                    if (isEditMode) R.string.edit_habit_save_button
                    else R.string.add_habit_create_button
                )
            ) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.add_habit_cancel_button)) }
        }
    )
}

/**
 * Simplified version for boolean habits (retrocompatibility)
 */
@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, icon: String, colorArgb: Int) -> Unit,
    initialName: String = "",
    initialIcon: String = "none",
    initialColor: Color? = null
) {
    AddHabitDialog(
        onDismiss = onDismiss,
        onConfirm = { name, icon, colorArgb, habitType, unit, targetValue, step ->
            onConfirm(name, icon, colorArgb)
        },
        initialName = initialName,
        initialIcon = initialIcon,
        initialColor = initialColor,
        initialHabitType = HabitType.BOOLEAN,
        initialUnit = null,
        initialTargetValue = null,
        initialStep = 1f
    )
}