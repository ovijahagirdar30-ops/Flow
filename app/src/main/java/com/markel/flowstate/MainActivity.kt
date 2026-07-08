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
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.markel.flowstate.components.FlowBottomBar
import com.markel.flowstate.components.PlaceholderScreen
import com.markel.flowstate.core.designsystem.theme.FlowStateTheme
import com.markel.flowstate.core.designsystem.ui.LocalAnimatedVisibilityScope
import com.markel.flowstate.core.designsystem.ui.LocalSharedTransitionScope
import com.markel.flowstate.core.data.MainTab
import com.markel.flowstate.feature.flow.tasks.util.HandleSystemBars
import com.markel.flowstate.navigation.BottomNavScreen
import com.markel.flowstate.navigation.FlowStateNavDisplay
import com.markel.flowstate.navigation.FlowStateSavedStateConfiguration
import com.markel.flowstate.navigation.MainTabsKey
import com.markel.flowstate.navigation.TabKey
import com.markel.flowstate.navigation.fromKey
import com.markel.flowstate.navigation.toKey
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val isReady by mainViewModel.isReady.collectAsState()
            val initialTab by mainViewModel.initialTab.collectAsState()
            val bottomNavOrder by mainViewModel.bottomNavOrder.collectAsState()
            val bottomNavHidden by mainViewModel.bottomNavHidden.collectAsState()
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by mainViewModel.dynamicColor.collectAsStateWithLifecycle()

            splashScreen.setKeepOnScreenCondition { !isReady }

            if (isReady) {
                FlowStateTheme(
                    themeMode = themeMode,
                    dynamicColor = dynamicColor
                ) {
                    // Check Orientation
                    val configuration = LocalConfiguration.current
                    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    HandleSystemBars(isLandscape)

                    // Build the dynamic bottom nav items based on user configuration.
                    val visibleBottomNavItems = remember(bottomNavOrder, bottomNavHidden) {
                        val screenMap = allBottomNavScreens.associateBy { MainTab.fromKey(it.key) }
                        bottomNavOrder
                            .filter { it !in bottomNavHidden }
                            .mapNotNull { screenMap[it] }
                    }

                    // Single mutable back stack. MainTabsKey is the immutable
                    // root at index 0; the active tab is at index 1; any
                    // fullscreen destinations sit on top.
                    //
                    // KNOWN LIMITATION (Phase 1): single back stack — switching
                    // tabs replaces the active tab and discards its scroll
                    // position and detail history. Multi-back-stack (one per
                    // top-level tab) will be added in Phase 2 to preserve tab
                    // state across switches.
                    val backStack: NavBackStack<NavKey> = rememberNavBackStack(
                        FlowStateSavedStateConfiguration,
                        MainTabsKey,
                        initialTab.toKey(),
                    )

                    // Persist the active tab whenever it changes — used as the
                    // start destination on next launch.
                    LaunchedEffect(backStack) {
                        snapshotFlow { backStack.lastOrNull { it is TabKey } as? TabKey }
                            .collect { tabKey ->
                                tabKey?.let { MainTab.fromKey(it) }?.let(mainViewModel::saveLastTab)
                            }
                    }

                    SharedTransitionLayout(
                        modifier = Modifier.fillMaxSize().clipToBounds()
                    ) {
                        FlowStateNavDisplay(
                            backStack = backStack,
                            bottomNavOrder = bottomNavOrder,
                            bottomNavHidden = bottomNavHidden,
                            onBottomNavConfigChanged = mainViewModel::saveBottomNavConfig,
                            themeMode = themeMode,
                            dynamicColor = dynamicColor,
                            onThemeModeChange = mainViewModel::saveThemeMode,
                            onDynamicColorChange = mainViewModel::saveDynamicColor,
                            sharedTransitionScope = this,
                            bottomBar = {
                                FlowBottomBar(
                                    backStack = backStack,
                                    onSwitchTab = { newTab ->
                                        // Switch top-level tab: keep MainTabsKey at index 0,
                                        // replace the active tab.
                                        // KNOWN LIMITATION: discards the previous tab's back
                                        // history. Multi-back-stack will be added in Phase 2.
                                        while (backStack.size > 1) {
                                            backStack.removeAt(backStack.lastIndex)
                                        }
                                        backStack.add(newTab)
                                    },
                                    isLandscape = isLandscape,
                                    items = visibleBottomNavItems,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

/** All possible bottom-nav screens, used as a lookup map by MainActivity. */
val allBottomNavScreens: List<BottomNavScreen> = listOf(
    BottomNavScreen.Tasks,
    BottomNavScreen.Calendar,
    BottomNavScreen.Habits,
    BottomNavScreen.Mood,
    BottomNavScreen.Settings,
)