package com.markel.flowstate.navigation

import androidx.annotation.StringRes
import com.markel.flowstate.R

sealed class Screen(val route: String, @StringRes val labelRes: Int, val iconRes: Int) {
    object Tasks : Screen("tasks", com.markel.flowstate.feature.tasks.R.string.flow, R.drawable.task_alt_24px)
    object Calendar : Screen("calendar", com.markel.flowstate.feature.tasks.R.string.calendar, R.drawable.calendar_today)
    object Habits : Screen("habits", com.markel.flowstate.feature.tasks.R.string.habits, R.drawable.calendar_month_24px)
    object Mood : Screen("mood", com.markel.flowstate.feature.tasks.R.string.mood, R.drawable.self_improvement_24px)

    // Routes that are full-screen without bottom bar
    object Detail {
        const val TASK_EDITOR = "task_editor/{taskId}"
        const val IDEA_EDITOR = "idea_editor/{ideaId}" // ideaId = "new" for creation
        const val CHECKLIST_EDITOR = "checklist_editor/{checkListId}"
        const val HABIT_DETAIL = "habit_detail/{habitId}"
        fun taskEditor(taskId: Int) = "task_editor/$taskId"
        fun ideaEditor(ideaId: Int) = "idea_editor/$ideaId"
        fun newIdea() = "idea_editor/new"
        fun checkListEditor(checkListId: Int?) =
            "checklist_editor/${checkListId ?: "new"}"
        fun habitDetail(habitId: Int) = "habit_detail/$habitId"
    }
}