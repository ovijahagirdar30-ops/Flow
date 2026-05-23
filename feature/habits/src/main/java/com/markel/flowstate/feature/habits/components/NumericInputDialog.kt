package com.markel.flowstate.feature.habits.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.markel.flowstate.feature.habits.R

@Composable
fun NumericInputDialog(
    habitName: String,
    unit: String?,
    currentValue: Float?,
    onDismiss: () -> Unit,
    onConfirm: (Float?) -> Unit
) {
    var valueText by remember {
        mutableStateOf(
            currentValue?.let {
                if (it == 0f) ""
                else if (it % 1 == 0f) it.toInt().toString()
                else it.toString()
            } ?: ""
        )
    }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.habit_input_dialog_title, habitName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { newValue ->
                        // Only allow numbers and a single decimal point
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            valueText = newValue
                        }
                    },
                    label = {
                        Text(
                            if (unit != null)
                                stringResource(R.string.habit_input_label_unit, unit)
                            else
                                stringResource(R.string.habit_input_label_plain)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val parsed = valueText.toFloatOrNull()
                            onConfirm(parsed)
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = valueText.toFloatOrNull()
                    onConfirm(parsed)
                },
                enabled = valueText.isNotBlank() && valueText.toFloatOrNull() != null
            ) {
                Text(stringResource(R.string.habit_input_confirm))
            }
        },
        dismissButton = {
            Row {
                if (currentValue != null && currentValue > 0f) {
                    TextButton(
                        onClick = { onConfirm(null) }
                    ) {
                        Text(
                            stringResource(R.string.habit_clean_amount),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.add_habit_cancel_button))
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        // Request focus on the text field when the dialog opens
        focusRequester.requestFocus()
    }
}