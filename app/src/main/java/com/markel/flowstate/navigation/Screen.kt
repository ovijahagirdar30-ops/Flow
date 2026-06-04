package com.markel.flowstate.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.markel.flowstate.R
import com.markel.flowstate.core.data.MainTab

import kotlinx.serialization.Serializable

@Serializable
object TasksRoute

@Serializable
object CalendarRoute

@Serializable
object HabitsRoute

@Serializable
object MoodRoute

@Serializable
object SettingsRoute

@Serializable
object AboutRoute

@Serializable
object BottomNavConfigRoute

@Serializable
data class TaskEditorRoute(val taskId: Int)

@Serializable
data class IdeaEditorRoute(val ideaId: Int? = null)

@Serializable
data class CheckListEditorRoute(val checkListId: Int? = null)

@Serializable
data class HabitDetailRoute(val habitId: Int)


fun MainTab.toRoute(): Any = when (this) {
    MainTab.TASKS -> TasksRoute
    MainTab.CALENDAR -> CalendarRoute
    MainTab.HABITS -> HabitsRoute
    MainTab.MOOD -> MoodRoute
    MainTab.SETTINGS -> SettingsRoute
}

fun MainTab.Companion.fromRoute(route: Any): MainTab? = when {
    route is TasksRoute || route::class == TasksRoute::class -> MainTab.TASKS
    route is CalendarRoute || route::class == CalendarRoute::class -> MainTab.CALENDAR
    route is HabitsRoute || route::class == HabitsRoute::class -> MainTab.HABITS
    route is MoodRoute || route::class == MoodRoute::class -> MainTab.MOOD
    route is SettingsRoute || route::class == SettingsRoute::class -> MainTab.SETTINGS
    else -> null
}


sealed class BottomNavScreen(
    val route: Any,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    @DrawableRes val iconSelectedRes: Int
) {
    object Tasks : BottomNavScreen(
        route = TasksRoute,
        labelRes = com.markel.flowstate.feature.tasks.R.string.flow,
        iconRes = R.drawable.task_alt_24px,
        iconSelectedRes = R.drawable.task_alt_24px
    )
    object Calendar : BottomNavScreen(
        route = CalendarRoute,
        labelRes = com.markel.flowstate.feature.tasks.R.string.calendar,
        iconRes = R.drawable.calendar_month_out_24px,
        iconSelectedRes = R.drawable.calendar_month_24px
    )
    object Habits : BottomNavScreen(
        route = HabitsRoute,
        labelRes = com.markel.flowstate.feature.tasks.R.string.habits,
        iconRes = R.drawable.analytics_out_24px,
        iconSelectedRes = R.drawable.analytics_24px
    )
    object Mood : BottomNavScreen(
        route = MoodRoute,
        labelRes = com.markel.flowstate.feature.tasks.R.string.mood,
        iconRes = R.drawable.self_improvement_24px,
        iconSelectedRes = R.drawable.self_improvement_24px
    )

    data object Settings : BottomNavScreen(
        route = SettingsRoute,
        labelRes = com.markel.flowstate.feature.tasks.R.string.settings,
        iconRes = R.drawable.settings_out_24px,
        iconSelectedRes = R.drawable.settings_24px
    )
}