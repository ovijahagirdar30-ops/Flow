package com.markel.flowstate.navigation

import androidx.navigation3.runtime.NavKey
import com.markel.flowstate.core.data.MainTab
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import androidx.navigation3.runtime.NavMetadataKey
import androidx.savedstate.serialization.SavedStateConfiguration

// ─────────────────────────────────────────────────────────────────────────────
// NavKey hierarchy
//
// Two layers, mirroring the architecture:
//   - Tab keys (MainTabsKey + each tab): rendered INSIDE the Scene Decorator
//     (i.e. on top of the bottom bar).
//   - Fullscreen keys (detail/editors/settings sub-screens): rendered OUTSIDE
//     the Scene Decorator (i.e. covering the bottom bar).
//
// The decorator decides which layer applies by reading the FullScreenKey
// metadata flag from each entry — see FlowStateSceneDecoratorStrategy.kt.
// ─────────────────────────────────────────────────────────────────────────────

/** Root key of the back stack. Always at index 0. The decorator wraps it with the bar. */
@Serializable
data object MainTabsKey : NavKey

/** Marker for the five bottom-nav tabs. They get decorated with the bottom bar. */
@Serializable
sealed interface TabKey : NavKey {
    @Serializable data object Tasks : TabKey
    @Serializable data object Calendar : TabKey
    @Serializable data object Habits : TabKey
    @Serializable data object Mood : TabKey
    @Serializable data object Settings : TabKey
}

/** Fullscreen destinations that must cover the bottom bar. */
@Serializable
sealed interface FullScreenKey : NavKey {
    @Serializable data class TaskEditor(val taskId: Int) : FullScreenKey
    @Serializable data class IdeaEditor(val ideaId: Int?, val categoryId: Int?) : FullScreenKey
    @Serializable data class CheckListEditor(val checkListId: Int?, val categoryId: Int?) : FullScreenKey
    @Serializable data class HabitDetail(val habitId: Int) : FullScreenKey

    @Serializable data object About : FullScreenKey
    @Serializable data object Appearance : FullScreenKey
    @Serializable data object BottomNavConfig : FullScreenKey
    @Serializable data object Integrations : FullScreenKey
    @Serializable data object Categories : FullScreenKey
}

// ─────────────────────────────────────────────────────────────────────────────
// MainTab ↔ TabKey mapping
// ─────────────────────────────────────────────────────────────────────────────

fun MainTab.toKey(): TabKey = when (this) {
    MainTab.TASKS -> TabKey.Tasks
    MainTab.CALENDAR -> TabKey.Calendar
    MainTab.HABITS -> TabKey.Habits
    MainTab.MOOD -> TabKey.Mood
    MainTab.SETTINGS -> TabKey.Settings
}

fun MainTab.Companion.fromKey(key: NavKey): MainTab? = when (key) {
    is TabKey.Tasks -> MainTab.TASKS
    is TabKey.Calendar -> MainTab.CALENDAR
    is TabKey.Habits -> MainTab.HABITS
    is TabKey.Mood -> MainTab.MOOD
    is TabKey.Settings -> MainTab.SETTINGS
    else -> null
}

// ─────────────────────────────────────────────────────────────────────────────
// Metadata — FullScreen flag
//
// A SceneDecoratorStrategy wraps EVERY scene unconditionally. To opt specific
// entries out of the bottom-bar decorator, we tag them with `FullScreenMeta`.
// The decorator inspects each entry's metadata and returns the scene unwrapped
// if the flag is present.
//
// The `fullScreenVerticalSlide()` helper in TransitionMetadata.kt combines this
// flag with the slide-up transition. Use it for every fullscreen destination.
// ─────────────────────────────────────────────────────────────────────────────

object FullScreenMeta : NavMetadataKey<Boolean>

// ─────────────────────────────────────────────────────────────────────────────
// SavedStateConfiguration
//
// Mandatory for back-stack serialization (process death restoration). Every
// back stack must declare a polymorphic serializers module covering every
// concrete NavKey subclass it can hold.
//
// We use a single shared configuration for the whole app since we only have
// one NavDisplay and one back stack.
// ─────────────────────────────────────────────────────────────────────────────

val FlowStateSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            // Root
            subclass(MainTabsKey::class, MainTabsKey.serializer())

            // Tabs
            subclass(TabKey.Tasks::class, TabKey.Tasks.serializer())
            subclass(TabKey.Calendar::class, TabKey.Calendar.serializer())
            subclass(TabKey.Habits::class, TabKey.Habits.serializer())
            subclass(TabKey.Mood::class, TabKey.Mood.serializer())
            subclass(TabKey.Settings::class, TabKey.Settings.serializer())

            // Fullscreen — parameterized
            subclass(FullScreenKey.TaskEditor::class, FullScreenKey.TaskEditor.serializer())
            subclass(FullScreenKey.IdeaEditor::class, FullScreenKey.IdeaEditor.serializer())
            subclass(FullScreenKey.CheckListEditor::class, FullScreenKey.CheckListEditor.serializer())
            subclass(FullScreenKey.HabitDetail::class, FullScreenKey.HabitDetail.serializer())

            // Fullscreen — objects
            subclass(FullScreenKey.About::class, FullScreenKey.About.serializer())
            subclass(FullScreenKey.Appearance::class, FullScreenKey.Appearance.serializer())
            subclass(FullScreenKey.BottomNavConfig::class, FullScreenKey.BottomNavConfig.serializer())
            subclass(FullScreenKey.Integrations::class, FullScreenKey.Integrations.serializer())
            subclass(FullScreenKey.Categories::class, FullScreenKey.Categories.serializer())
        }
    }
}
