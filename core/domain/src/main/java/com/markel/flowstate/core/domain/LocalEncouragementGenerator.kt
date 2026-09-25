package com.markel.flowstate.core.domain

import javax.inject.Inject

/**
 * Offline [EncouragementGenerator]: warm templates filled with the day's
 * real numbers. Deterministic per date — the same evening always reads the
 * same line — and the fallback backend for GeminiEncouragementGenerator
 * (same role LocalEveningPlanner plays for the evening plan).
 *
 * Tone follows the product philosophy: celebrate what closed, treat
 * everything still open as neutral material for tomorrow, never guilt.
 */
class LocalEncouragementGenerator @Inject constructor() : EncouragementGenerator {

    override suspend fun generate(stats: DayReviewStats): String {
        val done = stats.completedCount
        val open = stats.pendingCount + stats.pushedCount
        val templates = when {
            done > 0 && open == 0 -> fullyDone
            done > 0 -> someDone
            open > 0 -> nothingDone
            else -> noTasks
        }
        // Stable per date so re-opening the page the same night doesn't shuffle it.
        return templates[stats.date.hashCode().mod(templates.size)].replace("{n}", done.toString())
    }

    private companion object {
        val someDone = listOf(
            "You got {n} done today — that's {n} small wins stacked up. The rest isn't behind, it's just next. Rest well.",
            "Closed {n} today. Whatever's still open can wait for a fresher you. Sleep well.",
            "{n} in the bag today. Progress doesn't need to be loud — tonight, it just needs rest.",
        )
        val fullyDone = listOf(
            "Everything you set out to do is done — the whole list. Enjoy the quiet. Sleep well.",
            "Fully wrapped, nothing hanging over tomorrow's start. You earned the downtime. Good night.",
        )
        val nothingDone = listOf(
            "Not every day lands checkmarks, and today didn't have to. Tomorrow gets a clean slate. Rest up.",
            "Today asked for showing up more than finishing, and you did. The list will keep. Sleep well.",
        )
        val noTasks = listOf(
            "A quiet day with nothing hanging over you — rare, and worth keeping. Rest well.",
        )
    }
}
