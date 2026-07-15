package com.markel.flowstate.navigation

import androidx.navigation3.runtime.NavKey

// ─────────────────────────────────────────────────────────────────────────────
// FlowStateNavigator
//
// Single entry point for ALL navigation events in the app:
//
//   navigator.navigate(TabKey.Habits)  → switch top-level tab
//   navigator.navigate(FullScreenKey.HabitDetail(42))  → push detail on current tab
//   navigator.goBack()  → pop / exit
//
// Decision logic: if the key is a [TabKey], we switch tabs (preserving the
// target tab's existing stack). Otherwise, we push it onto the CURRENT tab's
// stack — so a fullscreen destination opened from the Habits tab belongs to
// the Habits stack, and backing out of it returns to Habits.
//
// Back behavior (exit-from-any-tab):
//   - If the current tab's top is NOT its root, pop it (close the detail).
//   - If the current tab's top IS its root (the user is on the tab's home
//     screen), return `false` so the system finishes the activity — the user
//     exits the app from whatever tab they're on.
//
// This is intentionally different from the official `navscenedecorator`
// recipe, which uses "exit-through-home" (always bounce back to the start
// tab before exiting). FlowState prefers the more direct exit-from-any-tab
// behavior — matches how the user expects back to work on a phone.
// ─────────────────────────────────────────────────────────────────────────────

class FlowStateNavigator(val state: NavigationState) {

    /**
     * Navigate to [route].
     *
     * - If [route] is a [TabKey], switches to that tab (preserves its stack).
     * - Otherwise pushes [route] onto the current tab's stack.
     */
    fun navigate(route: NavKey) {
        when (route) {
            is TabKey -> {
                // Switch tab — the target tab's existing back stack (if any)
                // is preserved because we only mutate `topLevelRoute`, not
                // the per-tab stacks.
                state.topLevelRoute = route
            }
            else -> {
                // Push onto the current tab's stack.
                state.backStacks[state.topLevelRoute]?.add(route)
            }
        }
    }

    /**
    * Back navigation.
    *
    * - If the current tab has a detail on top, pop it.
    * - If the current tab is at its root (no details), return `false` so the
    *   system finishes the activity — the user exits the app from any tab.
    *
    * Returns `true` if the navigator handled the back (popped a detail),
    * `false` if the system should handle it (exit the app).
    */
    fun goBack(): Boolean {
        val currentStack = state.backStacks[state.topLevelRoute]
            ?: return false
        if (currentStack.isEmpty()) return false

        val currentTop = currentStack.last()

        // If we're at the tab's root, let the system exit the app.
        if (currentTop == state.topLevelRoute) {
            return false
        }

        // Otherwise pop the current top (close the detail / fullscreen).
        currentStack.removeLastOrNull()
        return true
    }
}
