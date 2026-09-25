package com.markel.flowstate.core.domain

import java.time.LocalTime
import javax.inject.Inject

/**
 * Deterministic, network-free [EveningPlanner]. Exists so the plan display
 * screen (and any tests) can be built and verified BEFORE a backend exists,
 * and doubles as the offline fallback if Gemini is unreachable at check-in
 * time.
 *
 * Scheduling heuristic — simple on purpose, it's a placeholder:
 *  - anchors on WHEN the user actually checks in: the first block starts at
 *    the next 5-minute mark of the wall clock (no fixed 18:00),
 *  - opens with a decompress REST whose length is derived from the mood
 *    scores — the offline stand-in for Gemini sizing that rest itself,
 *  - up to 2-4 tasks (count adapts to the check-in's energy score), dinner at
 *    19:00 only when the check-in is still pre-dinner, unfinished habits after
 *    that, then a wind-down (only if the evening still has room) and the 11PM
 *    ritual close. Tone is deliberately guilt-free per the product philosophy.
 *
 * Block times are stored as zero-padded 24h "HH:mm" (wrapping past midnight);
 * the UI renders them on a 12-hour clock.
 *
 * [feedback] is accepted and IGNORED — the heuristic is deterministic, so a
 * regenerate returns the same shape; only visible when the fallback is the
 * active backend.
 */
class LocalEveningPlanner @Inject constructor() : EveningPlanner {

    override suspend fun generatePlan(snapshot: CheckinSnapshot, feedback: PlanFeedback?): EveningPlan {
        val energy = snapshot.checkin?.mood?.energy
        val lowEnergy = energy != null && energy <= 4
        val maxTasks = when {
            lowEnergy -> 2
            energy != null && energy >= 7 -> 4
            else -> 3
        }

        val tasks = snapshot.tasks
            .sortedByDescending { it.priority.ordinal } // HIGH first
            .take(maxTasks)
        val habits = snapshot.habits
            .filter { !it.isCompletedToday }
            .take(3)

        val blocks = mutableListOf<PlanBlock>()
        // The plan starts when the user actually checks in (next 5-minute
        // mark) — never at a fixed hour.
        var clock = startAnchorMinutes()

        // Post-check-in decompress: length derived from the mood scores.
        val decompress = decompressMinutes(snapshot)
        blocks += PlanBlock(
            startTime = format(clock),
            durationMinutes = decompress,
            title = "Settle in",
            reason = decompressReason(decompress),
            kind = PlanBlockKind.REST
        )
        clock += decompress + GAP_MINUTES

        // Tasks before dinner while freshest — split around a 19:00 dinner,
        // but only when the check-in is still pre-dinner; a late check-in
        // gets every task up front and no dinner block.
        val preDinner = clock < DINNER_MINUTES
        val (beforeDinner, afterDinner) =
            if (preDinner) tasks.splitAt(2) else tasks to emptyList()
        beforeDinner.forEachIndexed { i, task ->
            blocks += taskBlock(task, clock, first = i == 0, energy = energy)
            clock += TASK_MINUTES + GAP_MINUTES
        }

        if (preDinner && (tasks.isNotEmpty() || habits.isNotEmpty())) {
            clock = maxOf(clock, DINNER_MINUTES)
            blocks += PlanBlock(format(clock), MEAL_MINUTES, "Dinner", "Screen break — refuel before the rest of the evening", PlanBlockKind.MEAL)
            clock += MEAL_MINUTES + GAP_MINUTES
        }

        afterDinner.forEach { task ->
            blocks += taskBlock(task, clock, first = false, energy = energy)
            clock += TASK_MINUTES + GAP_MINUTES
        }

        if (tasks.isEmpty() && habits.isEmpty()) {
            blocks += PlanBlock(format(clock), 60, "All caught up", "Nothing left today — the evening is genuinely yours", PlanBlockKind.REST)
            clock += 60 + GAP_MINUTES
        }

        habits.forEach { habitWithStatus ->
            blocks += PlanBlock(
                startTime = format(clock),
                durationMinutes = HABIT_MINUTES,
                title = habitWithStatus.habit.name,
                reason = "Still open today — kept short so it still fits",
                kind = PlanBlockKind.HABIT,
                referenceId = habitWithStatus.habit.id
            )
            clock += HABIT_MINUTES + GAP_MINUTES
        }

        // Wind-down only when the evening still has room before the close;
        // a late check-in goes straight to the ritual.
        if (clock + WIND_DOWN_DURATION + GAP_MINUTES <= REFLECTION_MINUTES) {
            val windDownStart = maxOf(clock, WIND_DOWN_MINUTES)
            blocks += PlanBlock(format(windDownStart), WIND_DOWN_DURATION, "Wind down", "Low-effort rest before the close", PlanBlockKind.REST)
            clock = windDownStart + WIND_DOWN_DURATION + GAP_MINUTES
        }

        val reflectionStart = maxOf(clock, REFLECTION_MINUTES)
        blocks += PlanBlock(format(reflectionStart), 30, "Reflection", "11PM ritual close — review the day, no guilt", PlanBlockKind.REFLECTION)

        return EveningPlan(
            date = snapshot.date,
            generatedAtMillis = System.currentTimeMillis(),
            headline = headlineFor(energy, lowEnergy),
            blocks = blocks
        )
    }

    /**
     * Wall-clock anchor: the current time rounded UP to the next 5 minutes so
     * the first block starts just after the user actually finishes the
     * check-in. Stays on the same day when rounding would hit 24:00.
     */
    private fun startAnchorMinutes(): Int {
        val now = LocalTime.now()
        val minutes = now.hour * 60 + now.minute
        val rounded = ((minutes + 4) / 5) * 5
        return if (rounded >= MINUTES_PER_DAY) minutes else rounded
    }

    /**
     * Post-check-in decompress length — the offline stand-in for the AI
     * sizing that rest itself: heavily drained earns a long reset, a fine day
     * only a quick pause.
     */
    private fun decompressMinutes(snapshot: CheckinSnapshot): Int {
        val mood = snapshot.checkin?.mood ?: return DEFAULT_DECOMPRESS_MINUTES
        return when {
            mood.sleepiness >= 8 || mood.energy <= 2 -> 40
            mood.sleepiness >= 6 || mood.energy <= 4 || mood.stress >= 8 -> 30
            mood.sleepiness >= 4 || mood.energy <= 6 -> 20
            else -> 10
        }
    }

    private fun decompressReason(minutes: Int): String = when {
        minutes >= 30 -> "Proper reset first — you're running low"
        minutes == 20 -> "Ease down before getting going"
        else -> "Quick decompress, then into the evening"
    }

    private fun taskBlock(task: Task, start: Int, first: Boolean, energy: Int?): PlanBlock = PlanBlock(
        startTime = format(start),
        durationMinutes = TASK_MINUTES,
        title = task.title,
        reason = if (first) {
            "Start here while you're freshest" + (energy?.let { " (energy $it/10 tonight)" } ?: "")
        } else {
            "From today's remaining tasks"
        },
        kind = PlanBlockKind.TASK,
        referenceId = task.id
    )

    private fun headlineFor(energy: Int?, lowEnergy: Boolean): String = when {
        energy == null -> "Plan for this evening"
        lowEnergy -> "Easy evening — keeping it light (energy $energy/10)"
        energy >= 7 -> "Productive evening — you've got energy to spend"
        else -> "Balanced evening, paced to how you feel"
    }

    // Locale.ROOT so clock digits stay ASCII regardless of device locale.
    // % 1440 keeps an evening that runs past midnight a valid clock time.
    private fun format(minutes: Int): String {
        val withinDay = ((minutes % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
        return String.format(java.util.Locale.ROOT, "%02d:%02d", withinDay / 60, withinDay % 60)
    }

    private fun <T> List<T>.splitAt(index: Int): Pair<List<T>, List<T>> =
        take(index) to drop(index)

    private companion object {
        const val MINUTES_PER_DAY = 24 * 60
        const val DINNER_MINUTES = 19 * 60      // dinner not before 19:00
        const val WIND_DOWN_MINUTES = 22 * 60
        const val REFLECTION_MINUTES = 23 * 60  // the 11PM close
        const val WIND_DOWN_DURATION = 60
        const val TASK_MINUTES = 45
        const val HABIT_MINUTES = 20
        const val MEAL_MINUTES = 30
        const val GAP_MINUTES = 10
        const val DEFAULT_DECOMPRESS_MINUTES = 15
    }
}
