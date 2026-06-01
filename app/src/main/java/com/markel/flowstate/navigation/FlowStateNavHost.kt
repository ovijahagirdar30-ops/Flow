package com.markel.flowstate.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FlowStateNavHost(
    navController: NavHostController,
    startDestination: Any,
    modifier: Modifier = Modifier
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
