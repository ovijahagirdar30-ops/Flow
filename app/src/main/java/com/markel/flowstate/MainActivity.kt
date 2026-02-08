package com.markel.flowstate

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.markel.flowstate.feature.tasks.TaskScreen
import com.markel.flowstate.feature.tasks.TaskViewModel
import com.markel.flowstate.core.designsystem.theme.FlowStateTheme
import com.markel.flowstate.feature.calendar.CalendarScreen
import com.markel.flowstate.feature.calendar.CalendarViewModel
import com.markel.flowstate.feature.tasks.util.HandleSystemBars
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate

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

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        FlowBottomBar(
                            navController = navController,
                            isLandscape = isLandscape
                        )
                    },
                    // Adjusting insets if we hide native navigation bar
                    contentWindowInsets = if (isLandscape) WindowInsets(0.dp) else WindowInsets.navigationBars
                ) { innerPadding ->
                    // Navigation host: decides which screen to show
                    // based on the route
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Tasks.route, // Starting in Tasks
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // --- Here we define each screen ---
                        composable(Screen.Tasks.route) {
                            val taskViewModel: TaskViewModel = hiltViewModel()
                            // We pass the ViewModel to the tasks screen
                            TaskScreen(viewModel = taskViewModel)
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

@Composable
fun FlowBottomBar(navController: NavHostController, isLandscape: Boolean) {
    // We get the current route to know which item to select
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val barHeight = if (isLandscape) 56.dp else 110.dp  // Reduce height in landscape mode

    Column(modifier = Modifier.fillMaxWidth()) {
        // Divider to separate bottom bar from content, both have the same surface color
        HorizontalDivider(
            thickness = 0.3.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.height(barHeight)
        ) {
            bottomNavItems.forEach { screen ->
                val label = stringResource(screen.labelRes)
                NavigationBarItem(
                    icon = {
                        if (screen == Screen.Calendar) {
                        DynamicCalendarIcon()
                        }
                        else
                            Icon(imageVector = ImageVector.vectorResource(screen.iconRes), contentDescription = label)
                    },
                    label = if (!isLandscape) { { Text(label) } } else null,  // Hide labels in landscape mode
                    selected = currentRoute == screen.route,
                    onClick = {
                        // Navigate to the new screen
                        navController.navigate(screen.route) {
                            // Avoid accumulating screens in the back stack
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = MaterialTheme.colorScheme.tertiary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
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

@Composable
fun DynamicCalendarIcon() {
    val today = remember { LocalDate.now().dayOfMonth.toString() }

    Box(contentAlignment = Alignment.Center) {
        // Base icon
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.calendar_today),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )

        // Number day
        Text(
            text = today,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            // Adjust size to fit it inside the icon
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp, start = 1.dp)
        )
    }
}