package com.markel.flowstate.core.domain

import javax.inject.Inject

/**
 * Deterministic, network-free [EveningPlanner]. Exists so the plan display
 * screen (and any tests) can be built and verified BEFORE a backend exists,
 * and doubles as the offline fallback if Gemini is unreachable at check-in
 * time.
 *
 * Scheduling heuristic — simple on purpose, it's a placeholder:
 * evenings run 18:00 → 23:30; up to 2-4 tasks first (count adapts to the
 * check-in's energy score), dinner once the clock passes 19:00, unfinished
 * habits after that, then a wind-down and the 11PM ritual close. Tone is
 * deliberately guilt-free per the product philosophy.
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
        var clock = START_MINUTES

        // Tasks before dinner while freshest — split around a 19:00 dinner.
        val (beforeDinner, afterDinner) = tasks.splitAt(2)
        beforeDinner.forEachIndexed { i, task ->
            blocks += taskBlock(task, clock, first = i == 0, energy = energy)
            clock += TASK_MINUTES + GAP_MINUTES
        }

        if (tasks.isNotEmpty() || habits.isNotEmpty()) {
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

        val windDownStart = maxOf(clock, WIND_DOWN_MINUTES)
        blocks += PlanBlock(format(windDownStart), 60, "Wind down", "Low-effort rest before the close", PlanBlockKind.REST)
        blocks += PlanBlock(format(REFLECTION_MINUTES), 30, "Reflection", "11PM ritual close — review the day, no guilt", PlanBlockKind.REFLECTION)

        return EveningPlan(
            date = snapshot.date,
            generatedAtMillis = System.currentTimeMillis(),
            headline = headlineFor(energy, lowEnergy),
            blocks = blocks
        )
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
    private fun format(minutes: Int): String =
        String.format(java.util.Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60)

    private fun <T> List<T>.splitAt(index: Int): Pair<List<T>, List<T>> =
        take(index) to drop(index)

    private companion object {
        const val START_MINUTES = 18 * 60       // 18:00
        const val DINNER_MINUTES = 19 * 60      // dinner not before 19:00
        const val WIND_DOWN_MINUTES = 22 * 60
        const val REFLECTION_MINUTES = 23 * 60  // the 11PM close
        const val TASK_MINUTES = 45
        const val HABIT_MINUTES = 20
        const val MEAL_MINUTES = 30
        const val GAP_MINUTES = 10
    }
}
