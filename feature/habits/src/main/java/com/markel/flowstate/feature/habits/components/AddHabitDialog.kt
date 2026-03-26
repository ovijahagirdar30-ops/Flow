package com.markel.flowstate.feature.habits.components

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.markel.flowstate.feature.habits.R

private val habitColors = listOf(
    Color(0xFF6650A4), Color(0xFF0061A4), Color(0xFF006E1C),
    Color(0xFFB3261E), Color(0xFFE8710A), Color(0xFF006A6A),
    Color(0xFF6B5778), Color(0xFF386666)
)

@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, icon: String, colorArgb: Int) -> Unit,
    initialName: String = "",
    initialIcon: String = "none",
    initialColor: Color? = null
) {
    val isEditMode = initialName.isNotEmpty() || initialColor != null
    var name by remember { mutableStateOf(initialName) }
    var selectedIcon by remember { mutableStateOf(initialIcon) }
    var selectedColor by remember { mutableStateOf( initialColor?.let { ic -> habitColors.firstOrNull { it == ic } } ?: habitColors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(
            stringResource(
                if (isEditMode) R.string.edit_habit_dialog_title
                else R.string.add_habit_dialog_title
            )
        ) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.add_habit_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
                        modifier = Modifier.height(80.dp)
                    ) {
                        items(habitColors) { color ->
                            val isSelected = color == selectedColor
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
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
                onClick = { onConfirm(name, selectedIcon, selectedColor.toArgb()) },
                enabled = name.isNotBlank()
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