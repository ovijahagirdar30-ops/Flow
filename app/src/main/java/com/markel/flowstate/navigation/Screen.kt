package com.markel.flowstate.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.markel.flowstate.R

/**
 * Bottom-nav items metadata (label + icon resources) keyed by [TabKey].
 *
 * The actual NavKey definitions live in [NavKeys.kt]. This sealed class is
 * just a UI-facing descriptor used by [com.markel.flowstate.components.FlowBottomBar]
 * to render each tab.
 */
sealed class BottomNavScreen(
    val key: TabKey,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    @DrawableRes val iconSelectedRes: Int
) {
    object Tasks : BottomNavScreen(
        key = TabKey.Tasks,
        labelRes = com.markel.flowstate.feature.tasks.R.string.flow,
        iconRes = R.drawable.task_alt_24px,
        iconSelectedRes = R.drawable.task_alt_24px
    )
    object Calendar : BottomNavScreen(
        key = TabKey.Calendar,
        labelRes = com.markel.flowstate.feature.tasks.R.string.calendar,
        iconRes = R.drawable.calendar_month_out_24px,
        iconSelectedRes = R.drawable.calendar_month_24px
    )
    object Habits : BottomNavScreen(
        key = TabKey.Habits,
        labelRes = com.markel.flowstate.feature.tasks.R.string.habits,
        iconRes = R.drawable.analytics_out_24px,
        iconSelectedRes = R.drawable.analytics_24px
    )
    object Mood : BottomNavScreen(
        key = TabKey.Mood,
        labelRes = com.markel.flowstate.feature.tasks.R.string.mood,
        iconRes = R.drawable.self_improvement_24px,
        iconSelectedRes = R.drawable.self_improvement_24px
    )

    data object Settings : BottomNavScreen(
        key = TabKey.Settings,
        labelRes = com.markel.flowstate.feature.tasks.R.string.settings,
        iconRes = R.drawable.settings_out_24px,
        iconSelectedRes = R.drawable.settings_24px
    )
}
