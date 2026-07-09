package com.markel.flowstate.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

// ─────────────────────────────────────────────────────────────────────────────
// NavigationState — per-tab back stacks
//
// Adapted from the official `navscenedecorator` recipe:
// https://github.com/android/nav3-recipes/tree/main/app/src/main/java/com/example/nav3recipes/navscenedecorator
//
// Behavior:
//     keeps ONE NavBackStack PER top-level tab. Switching tabs just
//     changes `topLevelRoute`; each tab's stack (and the saveable state of
//     every entry inside it) is preserved intact.
//
// `stacksInUse` returns ONLY `[topLevelRoute]` (the active tab). Non-active
// tabs are NOT composed but their state IS retained (the NavBackStack
// instances live in `backStacks`). This is intentionally different from the
// official recipe (which composes `[startRoute, topLevelRoute]` to support
// the "exit through home" pattern) — FlowState uses "exit from any tab"
// instead, so we don't need to keep the start tab composed.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Creates the navigation state: one [NavBackStack] per top-level tab, plus a
 * saveable [topLevelRoute] that survives config changes and process death.
 *
 * @param initialRoute The tab to start on. Only used to seed [NavigationState.topLevelRoute]
 *                     on first composition; subsequent launches restore the
 *                     persisted value via `rememberSerializable`.
 * @param topLevelRoutes The set of all top-level tab keys. Each one gets its
 *                       own [NavBackStack] in [NavigationState.backStacks].
 */
@Composable
fun rememberNavigationState(
    initialRoute: TabKey,
    topLevelRoutes: Set<TabKey>,
): NavigationState {
    val topLevelRoute = rememberSerializable(
        initialRoute, topLevelRoutes,
        serializer = MutableStateSerializer(NavKeySerializer()),
    ) {
        mutableStateOf<NavKey>(initialRoute)
    }

    // One back stack per top-level tab. Each is remembered individually so it
    // survives recomposition and process death.
    val backStacks: Map<NavKey, NavBackStack<NavKey>> =
        topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }

    return remember(initialRoute, topLevelRoutes) {
        NavigationState(
            initialRoute = initialRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks,
        )
    }
}

/**
 * State holder for navigation. Does NOT mutate itself — mutations go through
 * [FlowStateNavigator].
 */
class NavigationState(
    val initialRoute: TabKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    /** The currently active top-level tab. Mutated by [FlowStateNavigator.navigate]. */
    var topLevelRoute: NavKey by topLevelRoute

    /**
     * The stacks currently being composed by NavDisplay.
     *
     * Only the active tab — non-active tabs retain their state (the
     * NavBackStack instances live in [backStacks]) but are not composed.
     */
    val stacksInUse: List<NavKey>
        get() = listOf(topLevelRoute)

    /**
     * Convert the navigation state into a flat list of decorated [NavEntry]s
     * for `NavDisplay(entries = ...)`.
     *
     * Each tab's stack gets its own `SaveableStateHolder` decorator via
     * [rememberDecoratedNavEntries] — this is the mechanism that preserves
     * per-tab `rememberSaveable` state when the tab is off-screen.
     */
    @Composable
    fun toEntries(
        entryProvider: (NavKey) -> NavEntry<NavKey>,
    ): SnapshotStateList<NavEntry<NavKey>> {
        val decoratedEntries = backStacks.mapValues { (_, stack) ->
            val decorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                rememberViewModelStoreNavEntryDecorator()
            )
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = decorators,
                entryProvider = entryProvider,
            )
        }

        return stacksInUse
            .flatMap { decoratedEntries[it] ?: emptyList() }
            .toMutableStateList()
    }
}
