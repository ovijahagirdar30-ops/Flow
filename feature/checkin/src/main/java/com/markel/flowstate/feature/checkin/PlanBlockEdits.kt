package com.markel.flowstate.feature.checkin

import com.markel.flowstate.core.domain.PlanBlock

/**
 * Result of a plan edit: the re-sorted block list plus the user's ticked
 * indexes remapped onto it.
 */
data class PlanEditResult(
    val blocks: List<PlanBlock>,
    val checkedIndexes: Set<Int>,
)

/**
 * Pure block-edit operations behind the Plan tab and the check-in plan step:
 * edit a block's time/details, remove one, or insert a new one.
 *
 * Two invariants these functions own, so no caller has to think about them:
 *
 *  1. **Time order** — the AI produces startTime-sorted blocks and the UI
 *     renders them as a timeline, so every edit re-sorts (stable: equal
 *     startTimes keep their relative order — a new block slots in AFTER an
 *     existing block with the same time).
 *
 *  2. **Tick remapping** — ticks are persisted as block INDEXES
 *     (`evening_plans.checkedIndexesJson`). Moving/removing/inserting shifts
 *     every position after it, so ticks are carried along by tracking each
 *     block's original index through the sort: a ticked block that slides
     * from position 3 to 1 stays ticked at position 1, and removing an
 *     unticked block ahead of a ticked one decrements its index.
 *
 * All functions are total: an out-of-range index returns the input
 * unchanged, and [PlanBlock]s are treated as immutable values (the edited
 * block keeps its identity tag, so an edit never silently unticks).
 */
internal object PlanBlockEdits {

    /** Replaces the block at [index] with [newBlock], re-sorting and remapping ticks. */
    fun replace(
        blocks: List<PlanBlock>,
        checked: Set<Int>,
        index: Int,
        newBlock: PlanBlock,
    ): PlanEditResult {
        if (index !in blocks.indices) return PlanEditResult(blocks, checked)
        val tagged = blocks.mapIndexed { i, block -> (if (i == index) newBlock else block) to i }
        return tagged.toResult(checked)
    }

    /** Removes the block at [index]; its tick (if any) disappears with it. */
    fun remove(
        blocks: List<PlanBlock>,
        checked: Set<Int>,
        index: Int,
    ): PlanEditResult {
        if (index !in blocks.indices) return PlanEditResult(blocks, checked)
        val tagged = blocks.mapIndexedNotNull { i, block -> if (i == index) null else (block to i) }
        return tagged.toResult(checked)
    }

    /** Inserts [newBlock] in time order; the new block always starts unticked. */
    fun insert(
        blocks: List<PlanBlock>,
        checked: Set<Int>,
        newBlock: PlanBlock,
    ): PlanEditResult {
        // Tag -1 can never be in `checked`, so the inserted block is never ticked.
        val tagged = blocks.mapIndexed { i, block -> block to i } + (newBlock to -1)
        return tagged.toResult(checked)
    }

    /** Stable sort by startTime, then rebuild the tick set from original tags. */
    private fun List<Pair<PlanBlock, Int>>.toResult(checked: Set<Int>): PlanEditResult {
        val sorted = sortedBy { it.first.startTime }
        return PlanEditResult(
            blocks = sorted.map { it.first },
            checkedIndexes = sorted
                .mapIndexedNotNull { position, (_, originalIndex) ->
                    position.takeIf { originalIndex in checked }
                }
                .toSet(),
        )
    }

    // ── AI-memory notes ────────────────────────────────────────────────
    //
    // Every manual edit is recorded durably (recordFeedback) so LATER
    // evenings inherit it as a PAST CORRECTIONS line — the same channel as
    // typed regenerate comments. Wording lives here so both the Plan tab and
    // the check-in step log identical, AI-readable notes.

    /** "Changed \"Skincare\" to 21:30, 10 min" */
    fun editNote(block: PlanBlock): String =
        "Changed \"${block.title}\" to ${block.startTime}, ${block.durationMinutes} min"

    /** "Removed \"Watch series\" from the plan" */
    fun removeNote(block: PlanBlock): String =
        "Removed \"${block.title}\" from the plan"

    /** "Added \"Stretch\" at 22:00 (10 min)" */
    fun addNote(block: PlanBlock): String =
        "Added \"${block.title}\" at ${block.startTime} (${block.durationMinutes} min)"
}
