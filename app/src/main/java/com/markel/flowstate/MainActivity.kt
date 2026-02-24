package com.markel.flowstate

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
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
import androidx.navigation.compose.rememberNavController
import com.markel.flowstate.components.FlowBottomBar
import com.markel.flowstate.feature.flow.tasks.TaskScreen
import com.markel.flowstate.feature.flow.tasks.TaskViewModel
import com.markel.flowstate.core.designsystem.theme.FlowStateTheme
import com.markel.flowstate.feature.calendar.CalendarScreen
import com.markel.flowstate.feature.calendar.CalendarViewModel
import com.markel.flowstate.feature.flow.FlowScreen
import com.markel.flowstate.feature.flow.FlowViewModel
import com.markel.flowstate.feature.flow.tasks.util.HandleSystemBars
import dagger.hilt.android.AndroidEntryPoint

// We define our navigation routes
sealed class Screen(val route: String, @StringRes val labelRes: Int, val iconRes: Int) {
    object Tasks : Screen("tasks", com.markel.flowstate.feature.tasks.R.string.tasks, R.drawable.task_alt_24px)
    object Calendar : Screen("calendar", com.markel.flowstate.feature.tasks.R.string.calendar, R.drawable.calendar_today)
    object Habits : Screen("habits", com.markel.flowstate.feature.tasks.R.string.habits, R.drawable.calendar_month_24px)
    object Mood : Screen("mood", com.markel.flowstate.feature.tasks.R.string.mood, R.drawable.self_improvement_24px)
}

val bottomNavItems = listOf(
    Screen.Tasks,
    Screen.Calendar,
    Screen.Habits,
    Screen.Mood
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
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
                // Lifted state: any overlay in any screen can request to hide the bottom bar
                var isBottomBarVisible by remember { mutableStateOf(true) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        AnimatedVisibility(
                            visible = isBottomBarVisible,
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut()
                        ) {
                            FlowBottomBar(
                                navController = navController,
                                isLandscape = isLandscape
                            )
                        }
                    },
                    // Adjusting insets if we hide native navigation bar
                    contentWindowInsets = WindowInsets(0.dp)
                ) { innerPadding ->
                    // Navigation host: decides which screen to show
                    // based on the route
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Tasks.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // --- Here we define each screen ---
                        composable(Screen.Tasks.route) {
                            FlowScreen(
                                onOverlayOpened = { isBottomBarVisible = false },
                                onOverlayClosed = { isBottomBarVisible = true }
                            )
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
                    }
                }
            }
        }
    }
}



// Simple Composable for screens we haven't made yet
@Composable
fun PlaceholderScreen(text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.headlineMedium)
    }
}