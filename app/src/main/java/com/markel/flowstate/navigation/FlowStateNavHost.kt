package com.markel.flowstate.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.markel.flowstate.components.PlaceholderScreen
import com.markel.flowstate.core.designsystem.ui.LocalAnimatedVisibilityScope
import com.markel.flowstate.feature.calendar.CalendarScreen
import com.markel.flowstate.feature.calendar.CalendarViewModel
import com.markel.flowstate.feature.flow.FlowScreen
import com.markel.flowstate.feature.flow.FlowViewModel
import com.markel.flowstate.feature.flow.checklists.CheckListEditorScreen
import com.markel.flowstate.feature.flow.ideas.IdeaEditorScreen
import com.markel.flowstate.feature.flow.tasks.components.TaskEditorScreen
import com.markel.flowstate.feature.habits.HabitScreen
import com.markel.flowstate.feature.habits.details.HabitDetailScreen
import com.markel.flowstate.feature.settings.AboutScreen
import com.markel.flowstate.feature.settings.SettingsScreen
import com.markel.flowstate.BuildConfig
import com.markel.flowstate.core.data.MainTab
import com.markel.flowstate.core.data.ThemeMode
import com.markel.flowstate.feature.settings.BottomNavConfigScreen
import com.markel.flowstate.core.notifications.NotificationSettingsIntentProvider
import com.markel.flowstate.feature.settings.AppearanceScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FlowStateNavHost(
    navController: NavHostController,
    startDestination: Any,
    modifier: Modifier = Modifier,
    bottomNavOrder: List<MainTab> = MainTab.DEFAULT_ORDER,
    bottomNavHidden: Set<MainTab> = emptySet(),
    onBottomNavConfigChanged: (order: List<MainTab>, hidden: Set<MainTab>) -> Unit = { _, _ -> },
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<TasksRoute> {
            val flowViewModel: FlowViewModel = hiltViewModel()
            // Register as lifecycle observer so onResume() fires correctly.
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                lifecycleOwner.lifecycle.addObserver(flowViewModel)
                onDispose { lifecycleOwner.lifecycle.removeObserver(flowViewModel) }
            }
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                FlowScreen(
                    flowViewModel = flowViewModel,
                    onNavigateToTaskEditor = { taskId ->
                        navController.navigate(TaskEditorRoute(taskId))
                    },
                    onNavigateToIdeaEditor = { ideaId ->
                        navController.navigate(IdeaEditorRoute(ideaId))
                    },
                    onNavigateToNewIdea = {
                        navController.navigate(IdeaEditorRoute(null))
                    },
                    onNavigateToCheckListEditor = { checkListId ->
                        navController.navigate(CheckListEditorRoute(checkListId))
                    }
                )
            }
        }
        composable<CalendarRoute> {
            val calendarViewModel: CalendarViewModel = hiltViewModel()
            CalendarScreen(viewModel = calendarViewModel)
        }
        composable<HabitsRoute> {
            HabitScreen(
                onNavigateToDetail = { habitId ->
                    navController.navigate(HabitDetailRoute(habitId))
                }
            )
        }
        composable<MoodRoute> {
            PlaceholderScreen(stringResource(com.markel.flowstate.feature.tasks.R.string.mood))
        }
        composable<SettingsRoute> {
            val context = LocalContext.current
            val notificationSettingsProvider = remember {
                NotificationSettingsIntentProvider(context)
            }
            // Observe notification permission state — refresh when returning from settings
            var notificationsEnabled by remember {
                mutableStateOf(notificationSettingsProvider.isNotificationPermissionGranted())
            }

            // Refresh state when returning from system settings
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        notificationsEnabled =
                            notificationSettingsProvider.isNotificationPermissionGranted()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            SettingsScreen(
                appVersion = BuildConfig.VERSION_NAME,
                onNavigateToNotifications = {
                    notificationSettingsProvider.createIntent()?.let { intent ->
                        context.startActivity(intent)
                    }
                },
                onNavigateToAppearance = { navController.navigate(AppearanceRoute) },
                onNavigateToBottomNavConfig = {
                    navController.navigate(BottomNavConfigRoute)
                },
                onNavigateToAbout = { navController.navigate(AboutRoute) },
                notificationsEnabled = notificationsEnabled,
            )
        }
        composable<AboutRoute> {
            AboutScreen(
                appVersion = BuildConfig.VERSION_NAME,
                onBack = { navController.popBackStack() }
            )
        }
        composable<BottomNavConfigRoute> {
            BottomNavConfigScreen(
                currentOrder = bottomNavOrder,
                currentHidden = bottomNavHidden,
                onConfigChanged = onBottomNavConfigChanged,
                onBack = { navController.popBackStack() }
            )
        }
        composable<AppearanceRoute> {
            AppearanceScreen(
                currentThemeMode = themeMode,
                currentDynamicColor = dynamicColor,
                onThemeModeChange = onThemeModeChange,
                onDynamicColorChange = onDynamicColorChange,
                onBack = { navController.popBackStack() }
            )
        }

        composable<TaskEditorRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<TaskEditorRoute>()
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                TaskEditorScreen(
                    taskId = args.taskId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable<IdeaEditorRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<IdeaEditorRoute>()
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                IdeaEditorScreen(
                    ideaId = args.ideaId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable<CheckListEditorRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<CheckListEditorRoute>()
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                CheckListEditorScreen(
                    checkListId = args.checkListId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable<HabitDetailRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<HabitDetailRoute>()
            HabitDetailScreen(
                habitId = args.habitId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
