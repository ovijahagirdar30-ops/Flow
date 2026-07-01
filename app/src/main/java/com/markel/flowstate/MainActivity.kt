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
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import com.markel.flowstate.components.FlowBottomBar
import com.markel.flowstate.components.PlaceholderScreen
import com.markel.flowstate.core.designsystem.theme.FlowStateTheme
import com.markel.flowstate.core.designsystem.ui.LocalAnimatedVisibilityScope
import com.markel.flowstate.core.designsystem.ui.LocalSharedTransitionScope
import com.markel.flowstate.core.data.MainTab
import com.markel.flowstate.navigation.fromRoute
import com.markel.flowstate.feature.flow.tasks.util.HandleSystemBars
import com.markel.flowstate.navigation.FlowStateNavHost
import com.markel.flowstate.navigation.BottomNavScreen
import dagger.hilt.android.AndroidEntryPoint

// All possible bottom nav screens, used as a lookup map
private val allBottomNavScreens = listOf(
    BottomNavScreen.Tasks,
    BottomNavScreen.Calendar,
    BottomNavScreen.Habits,
    BottomNavScreen.Mood,
    BottomNavScreen.Settings
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val startDestination by mainViewModel.startDestination.collectAsState()
            val bottomNavOrder by mainViewModel.bottomNavOrder.collectAsState()
            val bottomNavHidden by mainViewModel.bottomNavHidden.collectAsState()
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by mainViewModel.dynamicColor.collectAsStateWithLifecycle()

            splashScreen.setKeepOnScreenCondition {
                startDestination == null
            }

            if (startDestination != null) {
                FlowStateTheme(
                    themeMode = themeMode,
                    dynamicColor = dynamicColor
                ) {
                    // Check Orientation
                    val configuration = LocalConfiguration.current
                    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                    HandleSystemBars(isLandscape)
                    // Compose navigation controller
                    val navController = rememberNavController()
                    // The bottom bar is controlled BY ROUTE, not by manual state
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val destination = navBackStackEntry?.destination

                    // Build the dynamic bottom nav items based on user configuration
                    val visibleBottomNavItems = remember(bottomNavOrder, bottomNavHidden) {
                        val screenMap = allBottomNavScreens.associateBy { screen ->
                            MainTab.fromRoute(screen.route)
                        }
                        bottomNavOrder
                            .filter { it !in bottomNavHidden }
                            .mapNotNull { screenMap[it] }
                    }

                    // All routes (visible + hidden) where the bottom bar should be shown
                    val routesWithBottomBar = remember(bottomNavOrder) {
                        val screenMap = allBottomNavScreens.associateBy { screen ->
                            MainTab.fromRoute(screen.route)
                        }
                        bottomNavOrder.mapNotNull { screenMap[it]?.route }.toSet()
                    }

                    // Navigation 2.8 Type-Safe routes generate fully qualified class names
                    val isBottomBarVisible = routesWithBottomBar.any { destination?.hasRoute(it::class) == true }
                    // We show the bottom bar only if the current route is one of the main tabs
                    LaunchedEffect(destination) {
                        if (isBottomBarVisible && destination != null) {
                            val activeItem = allBottomNavScreens.firstOrNull { destination.hasRoute(it.route::class) }
                            if (activeItem != null) {
                                MainTab.fromRoute(activeItem.route)?.let { mainViewModel.saveLastTab(it) }
                            }
                        }
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (isBottomBarVisible) {
                                FlowBottomBar(
                                    navController = navController,
                                    isLandscape = isLandscape,
                                    items = visibleBottomNavItems
                                )
                            }
                        },
                        contentWindowInsets = WindowInsets(0.dp)
                    ) { innerPadding ->
                        SharedTransitionLayout(
                            modifier = Modifier
                                .padding(innerPadding)
                                .clipToBounds()
                        ) {
                            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                                // Navigation host: decides which screen to show based on the route
                                FlowStateNavHost(
                                    navController = navController,
                                    startDestination = startDestination!!,
                                    bottomNavOrder = bottomNavOrder,
                                    bottomNavHidden = bottomNavHidden,
                                    onBottomNavConfigChanged = { order, hidden ->
                                        mainViewModel.saveBottomNavConfig(order, hidden)
                                    },
                                    themeMode = themeMode,
                                    dynamicColor = dynamicColor,
                                    onThemeModeChange = { mainViewModel.saveThemeMode(it) },
                                    onDynamicColorChange = { mainViewModel.saveDynamicColor(it) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}