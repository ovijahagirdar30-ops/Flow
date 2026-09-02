package com.markel.flowstate.core.domain

/**
 * A single item worth showing at check-in time — could be a Task or a Habit.
 * Deliberately minimal for now (just enough to display a list); this is the
 * shape the future AI plan-generator will eventually read from too, so the
 * combining logic lives here once rather than being duplicated per-screen.
 */
enum class CheckinItemType { TASK, HABIT }

data class CheckinItem(
    val type: CheckinItemType,
    val sourceId: Int, // the underlying Task.id or Habit.id
    val title: String,
    val isCompleted: Boolean
)