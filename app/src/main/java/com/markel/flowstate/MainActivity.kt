package com.markel.flowstate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markel.flowstate.components.FlowBottomBar
import com.markel.flowstate.components.PlaceholderScreen
import com.markel.flowstate.core.data.AppColor
import com.markel.flowstate.core.designsystem.theme.FlowStateTheme
import com.markel.flowstate.core.designsystem.ui.LocalAnimatedVisibilityScope
import com.markel.flowstate.core.designsystem.ui.LocalSharedTransitionScope
import com.markel.flowstate.core.data.MainTab
import com.markel.flowstate.core.notifications.CheckinAlarmScheduler
import com.markel.flowstate.core.notifications.HomeGeofenceManager
import com.markel.flowstate.feature.checkin.CheckinActivity
import com.markel.flowstate.feature.checkin.HomeLocation
import com.markel.flowstate.feature.flow.tasks.util.HandleSystemBars
import com.markel.flowstate.navigation.BottomNavScreen
import com.markel.flowstate.navigation.FlowStateNavDisplay
import com.markel.flowstate.navigation.FlowStateNavigator
import com.markel.flowstate.navigation.NavigationState
import com.markel.flowstate.navigation.TabKey
import com.markel.flowstate.navigation.fromKey
import com.markel.flowstate.navigation.rememberNavigationState
import com.markel.flowstate.navigation.toKey
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var checkinAlarmScheduler: CheckinAlarmScheduler

    @Inject
    lateinit var homeGeofenceManager: HomeGeofenceManager

    // Must be registered as fields (before STARTED), not inside onCreate.

    private val fineLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            requestBackgroundLocationIfNeeded()
        } else {
            Toast.makeText(
                this,
                "Location permission denied — the arrival check-in trigger will not work.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            homeGeofenceManager.registerHomeGeofence(
                HomeLocation.LATITUDE, HomeLocation.LONGITUDE, HomeLocation.RADIUS_METERS
            )
        } else {
            Toast.makeText(
                this,
                "Background location denied — the arrival check-in trigger will not work.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Background location must be requested as a SEPARATE call after
     * foreground is granted (Android 11+ requirement) — never combined
     * into one request. Pre-Android 10, background location is bundled
     * with fine location automatically, so there's nothing extra to ask.
     */
    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            homeGeofenceManager.registerHomeGeofence(
                HomeLocation.LATITUDE, HomeLocation.LONGITUDE, HomeLocation.RADIUS_METERS
            )
        }
    }

    private fun hasBackgroundLocationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

    /**
     * Registers the home geofence if both permissions are already granted,
     * otherwise kicks off the two-step request flow. Safe to call every
     * launch — re-registers idempotently once granted, and won't spam a
     * dialog if the user already permanently denied it.
     */
    private fun requestLocationPermissionsAndRegisterGeofence() {
        if (hasBackgroundLocationPermission()) {
            homeGeofenceManager.registerHomeGeofence(
                HomeLocation.LATITUDE, HomeLocation.LONGITUDE, HomeLocation.RADIUS_METERS
            )
        } else {
            fineLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()        // Clear any cutoff alarm queued by older builds (the old arrival
        // fallback is gone), then arm tonight's9PM night-review page — its
        // receiver re-arms it after every fire.
        checkinAlarmScheduler.cancelFallbackCutoff()
        checkinAlarmScheduler.scheduleNightReview()

        // Debug-only on-demand triggers (no UI):
        //   --ez testCheckin true       arrival check-in pipeline in10s
        //   --ez testNightReview true   night review in10s (via the real alarm path)
        //   --ez openNight true         night review immediately (skips the alarm)
        // each: adb shell am start -n com.markel.flowstate/.MainActivity --ez <name> true
        if (BuildConfig.DEBUG && intent.getBooleanExtra("testCheckin", false)) {
            checkinAlarmScheduler.scheduleTest(10)
        }
        if (BuildConfig.DEBUG && intent.getBooleanExtra("testNightReview", false)) {
            checkinAlarmScheduler.scheduleNightReviewInTest(10)
        }
        if (BuildConfig.DEBUG && intent.getBooleanExtra("openNight", false)) {
            startActivity(Intent(this, CheckinActivity::class.java).apply {
                putExtra(CheckinActivity.EXTRA_MODE, CheckinActivity.MODE_NIGHT_REVIEW)
            })
        }

        // Register the home geofence (requests permissions if needed)
        requestLocationPermissionsAndRegisterGeofence()

        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val isReady by mainViewModel.isReady.collectAsState()
            val initialTab by mainViewModel.initialTab.collectAsState()
            val bottomNavOrder by mainViewModel.bottomNavOrder.collectAsState()
            val bottomNavHidden by mainViewModel.bottomNavHidden.collectAsState()
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by mainViewModel.dynamicColor.collectAsStateWithLifecycle()
            val pureSurfaces by mainViewModel.pureSurfaces.collectAsStateWithLifecycle()
            val systemFont by mainViewModel.systemFont.collectAsStateWithLifecycle()
            val selectedAppColor by mainViewModel.selectedAppColor.collectAsStateWithLifecycle()

            splashScreen.setKeepOnScreenCondition { !isReady }

            if (isReady) {
                FlowStateTheme(
                    themeMode = themeMode,
                    dynamicColor = dynamicColor,
                    pureSurfaces = pureSurfaces,
                    systemFont = systemFont,
                    selectedAppColor = selectedAppColor
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

                    // The set of all top-level tabs — each gets its own NavBackStack.
                    val topLevelRoutes: Set<TabKey> = remember(bottomNavOrder, bottomNavHidden) {
                        bottomNavOrder
                            .filter { it !in bottomNavHidden }
                            .map { it.toKey() }
                            .toSet()
                    }

                    // Per-tab back stacks. Each visible tab has its own NavBackStack.
                    // Switching tabs preserves the target tab's scroll position and detail history.
                    //
                    // `initialRoute` is the persisted last tab from DataStore —
                    // used only to seed `topLevelRoute` on first composition.
                    val navigationState: NavigationState = rememberNavigationState(
                        initialRoute = initialTab.toKey(),
                        topLevelRoutes = topLevelRoutes,
                    )
                    val navigator = remember(navigationState) { FlowStateNavigator(navigationState) }

                    // The check-in's Agree hand-off passes EXTRA_OPEN_TAB (a
                    // MainTab name) so the app opens straight on the Plan
                    // checklist tab. Only honored on a fresh launch — on
                    // recreation (rotation) the extra is ignored so the user's
                    // current tab survives.
                    val openTabOverride = remember(savedInstanceState) {
                        if (savedInstanceState == null) {
                            intent.getStringExtra(CheckinActivity.EXTRA_OPEN_TAB)
                                ?.let { MainTab.fromNameOrNull(it) }
                        } else {
                            null
                        }
                    }

                    // On first composition, switch to the hand-off tab if one
                    // was requested, else the persisted initial tab
                    // (rememberSerializable restores topLevelRoute = startRoute by default).
                    LaunchedEffect(initialTab, topLevelRoutes, openTabOverride) {
                        val targetTab = openTabOverride ?: initialTab
                        if (targetTab.toKey() in topLevelRoutes &&
                            navigationState.topLevelRoute != targetTab.toKey()
                        ) {
                            navigationState.topLevelRoute = targetTab.toKey()
                        }
                    }

                    // Persist the active top-level tab whenever it changes.
                    LaunchedEffect(navigationState) {
                        snapshotFlow { navigationState.topLevelRoute as? TabKey }
                            .collect { tabKey ->
                                tabKey?.let { MainTab.fromKey(it) }?.let(mainViewModel::saveLastTab)
                            }
                    }

                    SharedTransitionLayout(
                        modifier = Modifier.fillMaxSize().clipToBounds()
                    ) {
                        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                            FlowStateNavDisplay(
                                navigationState = navigationState,
                                navigator = navigator,
                                bottomNavOrder = bottomNavOrder,
                                bottomNavHidden = bottomNavHidden,
                                onBottomNavConfigChanged = mainViewModel::saveBottomNavConfig,
                                themeMode = themeMode,
                                dynamicColor = dynamicColor,
                                pureSurfaces = pureSurfaces,
                                systemFont = systemFont,
                                selectedAppColor = selectedAppColor,
                                onThemeModeChange = mainViewModel::saveThemeMode,
                                onDynamicColorChange = mainViewModel::saveDynamicColor,
                                onPureSurfacesChange = mainViewModel::savePureSurfaces,
                                onSystemFontChange = mainViewModel::saveSystemFont,
                                onAppColorChange = mainViewModel::saveSelectedAppColor,
                                sharedTransitionScope = this,
                                bottomBar = {
                                    FlowBottomBar(
                                        topLevelRoute = navigationState.topLevelRoute,
                                        onNavigate = { key -> navigator.navigate(key) },
                                        isLandscape = isLandscape,
                                        items = visibleBottomNavItems
                                    )
                                },
                            )
                        }
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
    BottomNavScreen.Plan,
    BottomNavScreen.Settings,
)
