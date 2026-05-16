package com.markel.flowstate.core.widgets

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class ToggleHabitAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Read habitId and habitType from the state of the widget
        val prefs = androidx.glance.appwidget.state.getAppWidgetState(
            context, PreferencesGlanceStateDefinition, glanceId
        )
        val habitId = prefs[KEY_HABIT_ID] ?: return
        val habitType = prefs[KEY_HABIT_TYPE] ?: "BOOLEAN"

        // For the numeric habits (should be impossible, but in any case) open the app
        if (habitType == "NUMERIC") {
            val launchIntent = context.packageManager
                .getLaunchIntentForPackage(context.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            }
            return
        }

        // Inject repository
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            HabitWidgetEntryPoint::class.java
        )
        val repository = entryPoint.habitRepository()

        // Toggle and persist in the DB
        val today = LocalDate.now()
        repository.toggleEntry(habitId, today)

        // Read and update the widget preferences for the re-render
        val isNowCompleted = repository.getEntriesForHabit(habitId)
            .first()
            .contains(today)
        updateAppWidgetState(
            context,
            PreferencesGlanceStateDefinition,
            glanceId
        ) { prefs ->
            prefs.toMutablePreferences().apply {
                this[KEY_IS_COMPLETED] = isNowCompleted
            }
        }

        // Force redraw of the widget
        HabitWidget().update(context, glanceId)
    }
}
