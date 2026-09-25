package com.markel.flowstate.core.domain

/**
 * The night check-in's day review: every task bucketed by how today went.
 * Produced by GetDayReviewUseCase and rendered by the night review page.
 *
 * Buckets are due-date driven (the agreed split):
 *  - [completedToday] — done AND stamped with a completion time inside
 *    today (device-local day). Tasks completed yesterday stay out entirely;
 *    legacy done-tasks with no completion stamp are treated as unknown and
 *    also stay out. Newest first.
 *  - [pending] — not done, due today or earlier, or unscheduled. Overdue
 *    first (oldest first), then due-today, then unscheduled.
 *  - [pushedToTomorrow] — not done with a due date after today. Earliest first.
 */
data class DayReview(
    /** ISO yyyy-MM-dd the review covers (device-local day). */
    val date: String,
    val completedToday: List<Task>,
    val pending: List<Task>,
    val pushedToTomorrow: List<Task>,
) {
    /** Compact numbers handed to [EncouragementGenerator]. */
    fun stats(): DayReviewStats = DayReviewStats(
        date = date,
        completedCount = completedToday.size,
        completedTitles = completedToday.take(5).map { it.title },
        pendingCount = pending.size,
        pushedCount = pushedToTomorrow.size,
    )
}

/**
 * What actually happened today, as numbers — the input to the closing
 * message. Titles are capped so the prompt stays small.
 */
data class DayReviewStats(
    val date: String,
    val completedCount: Int,
    val completedTitles: List<String>,
    val pendingCount: Int,
    val pushedCount: Int,
)
