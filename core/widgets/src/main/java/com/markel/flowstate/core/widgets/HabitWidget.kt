package com.markel.flowstate.core.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.layout.wrapContentSize
import androidx.glance.text.TextAlign
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate

// Key preferences for the widget
val KEY_HABIT_ID = intPreferencesKey("habit_id")
val KEY_HABIT_NAME = stringPreferencesKey("habit_name")
val KEY_HABIT_ICON = stringPreferencesKey("habit_icon")
val KEY_HABIT_COLOR = intPreferencesKey("habit_color")
val KEY_IS_COMPLETED = booleanPreferencesKey("is_completed")
val KEY_HABIT_TYPE = stringPreferencesKey("habit_type")
val KEY_DAY_NUMBER = intPreferencesKey("day_number")

class HabitWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load data needed to render the AppWidget
        // First, get the repository via EntryPointAccessors
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            HabitWidgetEntryPoint::class.java
        )
        val repository = entryPoint.habitRepository()

        // Read the saved habitId in Preferences for this specific widget
        val prefs = androidx.glance.appwidget.state.getAppWidgetState(
            context, PreferencesGlanceStateDefinition, id
        )
        val habitId = prefs[KEY_HABIT_ID] ?: -1

        if (habitId != -1) {
            // Check via repository and update Preferences with fresh data
            withContext(Dispatchers.IO) {
                val habit = repository.getHabitById(habitId)
                val today = LocalDate.now()
                val isCompleted = repository.getEntriesForHabit(habitId)
                    .first()
                    .contains(today)

                if (habit != null) {
                    androidx.glance.appwidget.state.updateAppWidgetState(context, id) { mutablePrefs ->
                        mutablePrefs[KEY_HABIT_NAME] = habit.name
                        mutablePrefs[KEY_HABIT_ICON] = habit.iconName
                        mutablePrefs[KEY_HABIT_COLOR] = habit.colorArgb
                        mutablePrefs[KEY_IS_COMPLETED] = isCompleted
                        mutablePrefs[KEY_HABIT_TYPE] = habit.habitType.name
                        mutablePrefs[KEY_HABIT_ID] = habitId
                        mutablePrefs[KEY_DAY_NUMBER] = today.dayOfMonth

                    }
                }
            }
        }

        // Render it
        provideContent {
            GlanceTheme {
                HabitWidgetContent()
            }
        }
    }

    @Composable
    private fun HabitWidgetContent() {
        val prefs = currentState<Preferences>()
        val habitId = prefs[KEY_HABIT_ID] ?: -1
        val habitName = prefs[KEY_HABIT_NAME] ?: ""
        val iconName = prefs[KEY_HABIT_ICON] ?: "none"
        val isCompleted = prefs[KEY_IS_COMPLETED] ?: false
        val dayNumber = prefs[KEY_DAY_NUMBER] ?: LocalDate.now().dayOfMonth

        val backgColor = GlanceTheme.colors.primaryContainer
        val backgIconColor = GlanceTheme.colors.onPrimaryContainer

        val badgeColor =
            if (isCompleted) GlanceTheme.colors.primary
            else GlanceTheme.colors.primaryContainer
        val badgeTextColor =
            if (isCompleted) GlanceTheme.colors.onPrimary
            else GlanceTheme.colors.onPrimaryContainer

        Box(
            modifier = GlanceModifier.fillMaxSize().clickable(actionRunCallback<ToggleHabitAction>())
        ) {
            // pill tinted with theme color
            Image(
                provider = ImageProvider(R.drawable.pill),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                colorFilter = ColorFilter.tint(
                    backgColor
                )
            )

            // Day in the top right and badge if completed
            Box(
                modifier = GlanceModifier.fillMaxSize().padding(top = 16.dp, end = 12.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = GlanceModifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Image(
                            provider = ImageProvider(
                                R.drawable.widget_badge_done
                            ),
                            contentDescription = "Completed",
                            modifier = GlanceModifier.fillMaxSize(),
                            colorFilter = ColorFilter.tint(badgeColor)
                        )
                    }
                    Text(
                        text = dayNumber.toString(),
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }

            // Icon bottom left
            Box(
                modifier = GlanceModifier.fillMaxSize().padding(bottom = 22.dp, start = 13.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                val iconRes = IconMapper.getDrawableRes(iconName)
                if (iconRes != null) {
                    Image(
                        provider = ImageProvider(iconRes),
                        contentDescription = habitName,
                        modifier = GlanceModifier.size(28.dp),
                        colorFilter = ColorFilter.tint(backgIconColor)
                    )
                } else {
                    Text(
                        text = habitName.take(2).uppercase(),
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = backgIconColor
                        )
                    )
                }
            }
        }
    }
}
