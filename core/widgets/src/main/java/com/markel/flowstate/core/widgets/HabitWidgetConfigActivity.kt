package com.markel.flowstate.core.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.markel.flowstate.core.domain.Habit
import com.markel.flowstate.core.domain.HabitRepository
import com.markel.flowstate.core.domain.HabitType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class HabitWidgetConfigActivity : ComponentActivity() {

    @Inject lateinit var habitRepository: HabitRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Default value if the user cancels
        setResult(Activity.RESULT_CANCELED)

        val appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            val scope = rememberCoroutineScope()
            var habits by remember { mutableStateOf<List<Habit>>(emptyList()) }

            LaunchedEffect(Unit) {
                habits = habitRepository.getHabits().first().filter { it.habitType == HabitType.BOOLEAN }
            }

            MaterialTheme {
                HabitPickerScreen(
                    habits = habits,
                    onHabitSelected = { habit ->
                        scope.launch {
                            val glanceId = GlanceAppWidgetManager(this@HabitWidgetConfigActivity)
                                .getGlanceIdBy(appWidgetId)

                            val today = LocalDate.now()

                            val isCompletedToday = habitRepository.getEntriesForHabit(habit.id)
                                .first()
                                .contains(today)

                            updateAppWidgetState(
                                this@HabitWidgetConfigActivity,
                                PreferencesGlanceStateDefinition,
                                glanceId
                            ) { prefs ->
                                prefs.toMutablePreferences().apply {
                                    this[KEY_HABIT_ID] = habit.id
                                    this[KEY_HABIT_NAME] = habit.name
                                    this[KEY_HABIT_ICON] = habit.iconName
                                    this[KEY_HABIT_COLOR] = habit.colorArgb
                                    this[KEY_HABIT_TYPE] = habit.habitType.name
                                    this[KEY_IS_COMPLETED] = isCompletedToday
                                    this[KEY_DAY_NUMBER] = today.dayOfMonth
                                }
                            }

                            // Show the widget
                            HabitWidget().update(
                                this@HabitWidgetConfigActivity,
                                glanceId
                            )

                            // Return ok to the launcher
                            setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            )
                            finish()
                        }
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
}

@Composable
private fun HabitPickerScreen(
    habits: List<Habit>,
    onHabitSelected: (Habit) -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.choose_habit),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (habits.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_boolean_habits),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(habits) { habit ->
                    ListItem(
                        headlineContent = { Text(habit.name) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onHabitSelected(habit) }
                    )
                    HorizontalDivider()
                }
            }
        }
        TextButton(
            onClick = onCancel,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(stringResource(R.string.cancel))
        }
    }
}
