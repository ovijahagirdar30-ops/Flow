package com.markel.flowstate.core.data

enum class MainTab {
    TASKS,
    CALENDAR,
    HABITS,
    MOOD;

    companion object {
        fun fromName(name: String?): MainTab {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: TASKS
        }
    }
}
