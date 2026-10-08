package com.markel.flowstate.feature.checkin

import com.markel.flowstate.core.domain.PlanBlock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

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

    /** The disappearing warning shown when a save would double-book a time. */
    const val OVERLAP_MESSAGE = "Another task is scheduled during that time"

    /**
     * The block whose time range ([start, start + duration)) intersects
     * [candidate]'s, or null when the candidate fits in free time.
     *
     * [excludeIndex] lets an edit ignore the block being replaced — a block
     * never overlaps itself, so re-saving one at its own time is never a
     * conflict. Returns null (never a false positive) when either time is
     * unparseable, and clamps ranges at midnight: an evening block may not
     * run into the next day for overlap purposes, so 23:30 + 60min does NOT
     * collide with a 00:15 block.
     */
    fun findOverlap(
        candidate: PlanBlock,
        blocks: List<PlanBlock>,
        excludeIndex: Int = -1,
    ): PlanBlock? {
        val candidateStart = parseMinute(candidate.startTime) ?: return null
        val candidateEnd = endMinute(candidateStart, candidate.durationMinutes)
        blocks.forEachIndexed { index, other ->
            if (index == excludeIndex) return@forEachIndexed
            val otherStart = parseMinute(other.startTime) ?: return@forEachIndexed
            val otherEnd = endMinute(otherStart, other.durationMinutes)
            // Half-open ranges: a block ending at 20:30 may be followed by
            // one starting at 20:30 — back-to-back is fine, only a real
            // intersection is rejected.
            if (candidateStart < otherEnd && otherStart < candidateEnd) return other
        }
        return null
    }

    /**
     * True once [block]'s scheduled window (start + duration) has fully
     * elapsed on [planDate] at [nowMillis] — the Plan tab's tick gate: a
     * checkbox only unlocks after the block's own time budget is spent.
     *
     * Deliberately permissive on bad data: an unparseable time or date counts
     * as due, so a malformed block can never trap its checkbox forever.
     */
    fun isDue(planDate: String, block: PlanBlock, nowMillis: Long): Boolean {
        val start = parseMinute(block.startTime) ?: return true
        val date = runCatching { LocalDate.parse(planDate) }.getOrNull() ?: return true
        val endMillis = date.atStartOfDay()
            .plusMinutes((start + block.durationMinutes.coerceAtLeast(1)).toLong())
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return nowMillis >= endMillis
    }

    /**
     * The block's end time as zero-padded 24h "HH:mm" (may wrap past
     * midnight — 23:00 + 120 min → "01:00"), or null when its time is
     * unparseable. Display input for [formatPlanTime].
     */
    fun endTimeHhMm(block: PlanBlock): String? {
        val start = parseMinute(block.startTime) ?: return null
        val end = start + block.durationMinutes.coerceAtLeast(1)
        return String.format(java.util.Locale.ROOT, "%02d:%02d", (end / 60) % 24, end % 60)
    }

    /** "21:05" → minute-of-day, null on malformed input. */
    private fun parseMinute(hhMm: String): Int? = runCatching {
        val time = LocalTime.parse(hhMm.trim())
        time.hour * 60 + time.minute
    }.getOrNull()

    /** End minute-of-day, clamped to midnight so an evening block can't spill over. */
    private fun endMinute(start: Int, durationMinutes: Int): Int =
        (start + durationMinutes.coerceAtLeast(1)).coerceAtMost(MINUTES_PER_DAY)

    private const val MINUTES_PER_DAY = 24 * 60

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

    /**
     * Drag-reorder: moves the block at [from] to position [to]. The time
     * SLOTS stay where they are — the sorted startTime list is re-applied
     * positionally while block identities shift between them — so dragging
     * "Dinner" below "Skincare" swaps their times instead of breaking the
     * timeline's time order. Ticks follow their block (invariant 2).
     *
     * Returns the input unchanged (the SAME list instance) when either index
     * is out of range, when from == to, or when the shuffle would double-book
     * a time — a longer block landing in a shorter slot leaves no room for
     * its neighbour. Callers surface [OVERLAP_MESSAGE] for that last case.
     */
    fun move(
        blocks: List<PlanBlock>,
        checked: Set<Int>,
        from: Int,
        to: Int,
    ): PlanEditResult {
        if (from == to || from !in blocks.indices || to !in blocks.indices) {
            return PlanEditResult(blocks, checked)
        }
        val slots = blocks.map { it.startTime }
        val reordered = blocks.toMutableList().also { it.add(to, it.removeAt(from)) }
        val shuffled = reordered.mapIndexed { slot, block -> block.copy(startTime = slots[slot]) }
        shuffled.forEachIndexed { i, block ->
            if (findOverlap(block, shuffled, excludeIndex = i) != null) {
                return PlanEditResult(blocks, checked)
            }
        }
        // Identity remap: the moved block lands at `to`; everything between
        // from and to shifts one slot toward the hole it left.
        fun remap(original: Int): Int = when {
            original == from -> to
            original < from -> if (original >= to) original + 1 else original
            else -> (original - 1).let { shifted -> if (shifted >= to) shifted + 1 else shifted }
        }
        return PlanEditResult(
            blocks = shuffled,
            checkedIndexes = checked.map(::remap).toSet(),
        )
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
