package com.markel.flowstate

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.markel.flowstate.components.FlowBottomBar
import com.markel.flowstate.components.PlaceholderScreen
import com.markel.flowstate.feature.flow.tasks.TaskScreen
import com.markel.flowstate.feature.flow.tasks.TaskViewModel
import com.markel.flowstate.core.designsystem.theme.FlowStateTheme
import com.markel.flowstate.core.designsystem.ui.LocalAnimatedVisibilityScope
import com.markel.flowstate.core.designsystem.ui.LocalSharedTransitionScope
import com.markel.flowstate.feature.calendar.CalendarScreen
import com.markel.flowstate.feature.calendar.CalendarViewModel
import com.markel.flowstate.feature.flow.FlowScreen
import com.markel.flowstate.feature.flow.FlowViewModel
import com.markel.flowstate.feature.flow.ideas.IdeaEditorScreen
import com.markel.flowstate.feature.flow.tasks.components.TaskEditorScreen
import com.markel.flowstate.feature.flow.tasks.util.HandleSystemBars
import com.markel.flowstate.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint

val bottomNavItems = listOf(
    Screen.Tasks,
    Screen.Calendar,
    Screen.Habits,
    Screen.Mood
)

// Routes where the bottom bar should be displayed
private val routesWithBottomBar = bottomNavItems.map { it.route }.toSet()
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlowStateTheme {
                // Check Orientation
                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                HandleSystemBars(isLandscape)
                // Compose navigation controller
                val navController = rememberNavController()
                // The bottom bar is controlled BY ROUTE, not by manual state
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                // We show the bottom bar only if the current route is one of the main tabs
                val isBottomBarVisible = currentRoute in routesWithBottomBar

                SharedTransitionLayout {
                    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                if (isBottomBarVisible) {
                                    FlowBottomBar(
                                        navController = navController,
                                        isLandscape = isLandscape
                                    )
                                }
                            },
                            contentWindowInsets = WindowInsets(0.dp)
                        ) { innerPadding ->
                            // Navigation host: decides which screen to show
                            // based on the route
                            NavHost(
                                navController = navController,
                                startDestination = Screen.Tasks.route,
                                modifier = Modifier.padding(innerPadding)
                            ) {
                                // --- Main tabs ---
                                composable(Screen.Tasks.route) {
                                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                        FlowScreen(
                                            onNavigateToTaskEditor = { taskId ->
                                                navController.navigate(
                                                    Screen.Detail.taskEditor(
                                                        taskId
                                                    )
                                                )
                                            },
                                            onNavigateToIdeaEditor = { ideaId ->
                                                navController.navigate(
                                                    Screen.Detail.ideaEditor(
                                                        ideaId
                                                    )
                                                )
                                            },
                                            onNavigateToNewIdea = {
                                                navController.navigate(Screen.Detail.newIdea())
                                            }
                                        )
                                    }
                                }
                                composable(Screen.Calendar.route) {
                                    val calendarViewModel: CalendarViewModel = hiltViewModel()
                                    CalendarScreen(viewModel = calendarViewModel)
                                }
                                composable(Screen.Habits.route) {
                                    // Temporarily a placeholder
                                    PlaceholderScreen(stringResource(com.markel.flowstate.feature.tasks.R.string.habits))
                                }
                                composable(Screen.Mood.route) {
                                    PlaceholderScreen(stringResource(com.markel.flowstate.feature.tasks.R.string.mood))
                                }

                                // ── Detail screens (without bottom bar) ─────────────
                                composable(Screen.Detail.TASK_EDITOR) { backStackEntry ->
                                    val taskId = backStackEntry.arguments
                                        ?.getString("taskId")
                                        ?.toIntOrNull() ?: return@composable
                                    TaskEditorScreen(
                                        taskId = taskId,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable(Screen.Detail.IDEA_EDITOR) { backStackEntry ->
                                    val ideaIdArg = backStackEntry.arguments?.getString("ideaId")
                                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                    IdeaEditorScreen(
                                        ideaId = ideaIdArg?.toIntOrNull(), // null = new idea
                                        onBack = { navController.popBackStack() }
                                    )
                                        }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}