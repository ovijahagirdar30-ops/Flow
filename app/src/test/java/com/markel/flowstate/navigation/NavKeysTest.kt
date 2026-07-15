package com.markel.flowstate.navigation

import androidx.navigation3.runtime.NavKey
import com.markel.flowstate.core.data.MainTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the [NavKey] hierarchy mappers defined in [NavKeys.kt].
 *
 * These mappers are the bridge between the persistence layer ([MainTab]) and
 * the navigation3 layer ([TabKey]). They run on every cold start (to seed
 * [NavigationState.topLevelRoute] from [MainViewModel.initialTab]) and on
 * every bottom-bar tap (to map the selected [BottomNavScreen] back to a
 * [MainTab] for [MainViewModel.saveLastTab]).
 */
class NavKeysTest {

    // ── toKey() / fromKey() round-trip ─────────────────────────────────────────

    /**
     * For every tab in [MainTab], the round-trip
     * `MainTab.fromKey(tab.toKey())` must return the original tab.
     *
     * Catches: a missing branch in either `when` block, or a wrong pairing
     * (e.g. `MainTab.HABITS -> TabKey.Calendar`).
     */
    @Test
    fun toKey_roundTrip_returnsOriginalTab_forAllFiveTabs() {
        MainTab.entries.forEach { tab ->
            val mapped = tab.toKey()
            val back = MainTab.fromKey(mapped)
            assertEquals("Round-trip failed for $tab", tab, back)
        }
    }

    /**
     * `fromKey` must return `null` for any [FullScreenKey] — fullscreen keys
     * are not tabs and have no [MainTab] equivalent.
     *
     * Guards against: a `when` block that falls through to a default tab
     * instead of `null` when given a non-tab key.
     */
    @Test
    fun fromKey_returnsNull_forFullScreenKeys() {
        val fullScreenKeys = listOf<NavKey>(
            FullScreenKey.TaskEditor(taskId = 1),
            FullScreenKey.IdeaEditor(ideaId = null, categoryId = null),
            FullScreenKey.IdeaEditor(ideaId = 7, categoryId = 2),
            FullScreenKey.CheckListEditor(checkListId = null, categoryId = null),
            FullScreenKey.CheckListEditor(checkListId = 5, categoryId = 1),
            FullScreenKey.HabitDetail(habitId = 42),
            FullScreenKey.About,
            FullScreenKey.Appearance,
            FullScreenKey.BottomNavConfig,
            FullScreenKey.Integrations,
            FullScreenKey.Categories,
        )

        fullScreenKeys.forEach { key ->
            assertNull(
                "fromKey must return null for FullScreenKey $key",
                MainTab.fromKey(key)
            )
        }
    }

    /**
     * `fromKey` must return `null` for any [NavKey] that is not part of the
     * [TabKey] hierarchy — including foreign NavKey instances that might be
     * introduced by future features or tests.
     *
     * Guards against: a `when` that throws on unknown keys (would crash at
     * runtime when persistence hands back an unexpected value) or that
     * silently maps unknown keys to TASKS.
     */
    @Test
    fun fromKey_returnsNull_forForeignNavKey() {
        val foreignKey = object : NavKey {}
        assertNull(MainTab.fromKey(foreignKey))
    }

    // ── toKey() type correctness ─────────────────────────────────────────────────

    /**
     * Each [MainTab] must map to the exact [TabKey] subtype that corresponds
     * to it — not just "some TabKey".
     *
     * Catches: a `when` block where two [MainTab]s collapse onto the same
     * [TabKey] (e.g. `HABITS -> TabKey.Tasks, TASKS -> TabKey.Tasks`) — the
     * round-trip test would still pass for both, but the bottom bar would
     * highlight the wrong item.
     */
    @Test
    fun toKey_returnsCorrectTabKeySubtype_forEachMainTab() {
        assertTrue(MainTab.TASKS.toKey() is TabKey.Tasks)
        assertTrue(MainTab.CALENDAR.toKey() is TabKey.Calendar)
        assertTrue(MainTab.HABITS.toKey() is TabKey.Habits)
        assertTrue(MainTab.MOOD.toKey() is TabKey.Mood)
        assertTrue(MainTab.SETTINGS.toKey() is TabKey.Settings)
    }

    // ── Coverage sanity ─────────────────────────────────────────────────────────

    /**
     * Sanity check: the [MainTab] enum and the [TabKey] hierarchy must have
     * the same cardinality. If a tab is added to one but not the other, the
     * `when` blocks in [toKey] / [fromKey] become non-exhaustive and the
     * compiler may not catch it (because `fromKey` has an `else -> null`
     * branch).
     *
     * This test exists precisely so that adding `MainTab.X` without a matching
     * `TabKey.X` fails loudly here instead of silently routing users to TASKS.
     */
    @Test
    fun mainTab_and_tabKey_haveSameCardinality() {
        val tabKeys = listOf(
            TabKey.Tasks, TabKey.Calendar, TabKey.Habits, TabKey.Mood, TabKey.Settings
        )
        assertEquals(
            "MainTab and TabKey must have the same number of entries",
            MainTab.entries.size,
            tabKeys.size
        )
        // Every MainTab must map to a distinct TabKey (no two tabs collapse onto the same key)
        val distinctKeys = MainTab.entries.map { it.toKey() }.toSet()
        assertEquals(
            "Each MainTab must map to a distinct TabKey",
            MainTab.entries.size,
            distinctKeys.size
        )
    }
}
