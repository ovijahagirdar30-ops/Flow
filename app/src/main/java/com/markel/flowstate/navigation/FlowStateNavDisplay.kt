package com.markel.flowstate.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.markel.flowstate.BuildConfig
import com.markel.flowstate.components.PlaceholderScreen
import com.markel.flowstate.core.data.MainTab
import com.markel.flowstate.core.data.ThemeMode
import com.markel.flowstate.core.designsystem.ui.LocalAnimatedVisibilityScope
import com.markel.flowstate.core.notifications.NotificationSettingsIntentProvider
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
import com.markel.flowstate.feature.settings.AppearanceScreen
import com.markel.flowstate.feature.settings.BackupScreen
import com.markel.flowstate.feature.settings.BottomNavConfigScreen
import com.markel.flowstate.feature.settings.CategoriesScreen
import com.markel.flowstate.feature.settings.SettingsScreen

// ─────────────────────────────────────────────────────────────────────────────
// FlowStateNavDisplay
//
// The single NavDisplay for the whole app. Replaces the old FlowStateNavHost.
//
// Architecture (Nested Navigation via Scene Decorator):
//
//   SharedTransitionLayout  ← provides SharedTransitionScope to NavDisplay
//   └── NavDisplay
//       ├── sceneDecoratorStrategies = [FlowStateSceneDecoratorStrategy]
//       │   ├── non-fullscreen scenes → wrapped with Scaffold + bottom bar
//       │   └── fullscreen scenes (FullScreenMeta=true) → pass-through, edge-to-edge
//       │
//       └── entryProvider
//           ├── MainTabsKey           (root, decorated)
//           ├── TabKey.Tasks          (decorated)
//           ├── TabKey.Calendar       (decorated)
//           ├── TabKey.Habits         (decorated)
//           ├── TabKey.Mood           (decorated)
//           ├── TabKey.Settings       (decorated)
//           │
//           └── FullScreenKey.*       (NOT decorated, slide-up + fade)
//               ├── TaskEditor, IdeaEditor, CheckListEditor
//               ├── HabitDetail
//               └── About, Appearance, BottomNavConfig, Integrations, Categories
//
// The bottom bar lives in the Scene Decorator. When the user navigates to a
// fullscreen key, the decorator stops wrapping the scene and the bar gets
// covered by the new scene sliding in over it.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FlowStateNavDisplay(
    backStack: NavBackStack<NavKey>,
    bottomNavOrder: List<MainTab>,
    bottomNavHidden: Set<MainTab>,
    onBottomNavConfigChanged: (order: List<MainTab>, hidden: Set<MainTab>) -> Unit,
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    bottomBar: @Composable () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    modifier: Modifier = Modifier,
) {
    val sceneDecorator = rememberFlowStateSceneDecoratorStrategy(
        sharedTransitionScope = sharedTransitionScope,
        bottomBar = bottomBar,
    )

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator() // REQUIRED for hiltViewModel() to be entry-scoped (one VM per entry rather than one per Activity)
        ),
        sceneDecoratorStrategies = listOf(sceneDecorator),
        sharedTransitionScope = sharedTransitionScope,
        entryProvider = entryProvider {
            // ── ROOT (decorated, no-op shell) ───────────────────────────────
            // MainTabsKey is the immutable root of the back stack. It carries
            // no UI of its own — a tab entry is always on top of it. The
            // decorator wraps it with the bar, but since the visible scene is
            // always the tab on top, this entry's content is never actually
            // seen by the user.
            entry<MainTabsKey> { /* no-op shell */ }

            // ── TABS (decorated) ────────────────────────────────────────────
            entry<TabKey.Tasks>(metadata = fadeTransition()) {
                val flowViewModel: FlowViewModel = hiltViewModel()
                // Register as lifecycle observer so onResume() fires correctly.
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    lifecycleOwner.lifecycle.addObserver(flowViewModel)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(flowViewModel) }
                }
                val navScope = LocalNavAnimatedContentScope.current
                CompositionLocalProvider(LocalAnimatedVisibilityScope provides navScope) {
                    FlowScreen(
                        flowViewModel = flowViewModel,
                        onNavigateToTaskEditor = { taskId ->
                            backStack.add(FullScreenKey.TaskEditor(taskId))
                        },
                        onNavigateToIdeaEditor = { ideaId ->
                            backStack.add(FullScreenKey.IdeaEditor(ideaId, null))
                        },
                        onNavigateToNewIdea = { categoryId ->
                            backStack.add(FullScreenKey.IdeaEditor(null, categoryId))
                        },
                        onNavigateToCheckListEditor = { checkListId, categoryId ->
                            backStack.add(FullScreenKey.CheckListEditor(checkListId, categoryId))
                        },
                    )
                }
            }

            entry<TabKey.Calendar>(metadata = fadeTransition()) {
                val calendarViewModel: CalendarViewModel = hiltViewModel()
                CalendarScreen(viewModel = calendarViewModel)
            }

            entry<TabKey.Habits>(metadata = fadeTransition()) {
                HabitScreen(
                    onNavigateToDetail = { habitId ->
                        backStack.add(FullScreenKey.HabitDetail(habitId))
                    },
                )
            }

            entry<TabKey.Mood>(metadata = fadeTransition()) {
                PlaceholderScreen(stringResource(com.markel.flowstate.feature.tasks.R.string.mood))
            }

            entry<TabKey.Settings>(metadata = fadeTransition()) {
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
                        notificationSettingsProvider.createIntent()?.let { context.startActivity(it) }
                    },
                    onNavigateToAppearance = { backStack.add(FullScreenKey.Appearance) },
                    onNavigateToBottomNavConfig = { backStack.add(FullScreenKey.BottomNavConfig) },
                    onNavigateToCategories = { backStack.add(FullScreenKey.Categories) },
                    onNavigateToIntegrations = { backStack.add(FullScreenKey.Integrations) },
                    onNavigateToAbout = { backStack.add(FullScreenKey.About) },
                    notificationsEnabled = notificationsEnabled,
                )
            }

            // ── FULLSCREEN — Flow editors (shared transitions) ─────────────
            entry<FullScreenKey.TaskEditor>(
                metadata = fullScreenVerticalSlide()
            ) { key ->
                val navScope = LocalNavAnimatedContentScope.current
                CompositionLocalProvider(LocalAnimatedVisibilityScope provides navScope) {
                    TaskEditorScreen(
                        taskId = key.taskId,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
            }

            entry<FullScreenKey.IdeaEditor>(
                metadata = fullScreenVerticalSlide()
            ) { key ->
                val navScope = LocalNavAnimatedContentScope.current
                CompositionLocalProvider(LocalAnimatedVisibilityScope provides navScope) {
                    IdeaEditorScreen(
                        ideaId = key.ideaId,
                        categoryId = key.categoryId,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
            }

            entry<FullScreenKey.CheckListEditor>(
                metadata = fullScreenVerticalSlide()
            ) { key ->
                val navScope = LocalNavAnimatedContentScope.current
                CompositionLocalProvider(LocalAnimatedVisibilityScope provides navScope) {
                    CheckListEditorScreen(
                        checkListId = key.checkListId,
                        categoryId = key.categoryId,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
            }

            // ── FULLSCREEN — Habit detail ──────────────────────────────────
            entry<FullScreenKey.HabitDetail>(
                metadata = fullScreenVerticalSlide()
            ) { key ->
                HabitDetailScreen(
                    habitId = key.habitId,
                    onBack = { backStack.removeLastOrNull() },
                )
            }

            // ── FULLSCREEN — Settings sub-screens ──────────────────────────
            entry<FullScreenKey.About>(
                metadata = fullScreenVerticalSlide()
            ) {
                AboutScreen(
                    appVersion = BuildConfig.VERSION_NAME,
                    onBack = { backStack.removeLastOrNull() },
                )
            }

            entry<FullScreenKey.BottomNavConfig>(
                metadata = fullScreenVerticalSlide()
            ) {
                BottomNavConfigScreen(
                    currentOrder = bottomNavOrder,
                    currentHidden = bottomNavHidden,
                    onConfigChanged = onBottomNavConfigChanged,
                    onBack = { backStack.removeLastOrNull() },
                )
            }

            entry<FullScreenKey.Appearance>(
                metadata = fullScreenVerticalSlide()
            ) {
                AppearanceScreen(
                    currentThemeMode = themeMode,
                    currentDynamicColor = dynamicColor,
                    onThemeModeChange = onThemeModeChange,
                    onDynamicColorChange = onDynamicColorChange,
                    onBack = { backStack.removeLastOrNull() },
                )
            }

            entry<FullScreenKey.Integrations>(
                metadata = fullScreenVerticalSlide()
            ) {
                BackupScreen(onBack = { backStack.removeLastOrNull() })
            }

            entry<FullScreenKey.Categories>(
                metadata = fullScreenVerticalSlide()
            ) {
                CategoriesScreen(onBack = { backStack.removeLastOrNull() })
            }
        },
    )
}
