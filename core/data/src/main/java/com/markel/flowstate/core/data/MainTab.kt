package com.markel.flowstate.core.data

enum class MainTab(val isRemovable: Boolean = false) {
    TASKS(isRemovable = false),
    CALENDAR(isRemovable = true),
    HABITS(isRemovable = false),
    MOOD(isRemovable = true),
    SETTINGS(isRemovable = false);

    companion object {
        fun fromName(name: String?): MainTab {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: TASKS
        }
        fun fromNameOrNull(name: String): MainTab? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }

        /** Default order for bottom navigation tabs. */
        val DEFAULT_ORDER: List<MainTab> = entries
    }
}
