package com.markel.flowstate.feature.checkin

import com.markel.flowstate.core.domain.PlanBlock
import com.markel.flowstate.core.domain.PlanBlockKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanBlockEditsTest {

    private fun block(
        time: String,
        title: String = "Block at $time",
        duration: Int = 30,
    ) = PlanBlock(
        startTime = time,
        durationMinutes = duration,
        title = title,
        reason = "because",
        kind = PlanBlockKind.OTHER,
    )

    // ── replace ────────────────────────────────────────────────────────────

    @Test
    fun `replace keeps position when time unchanged`() {
        val blocks = listOf(block("19:00"), block("20:00"), block("21:00"))
        val result = PlanBlockEdits.replace(blocks, setOf(1), 1, block("20:00", "Edited"))

        assertEquals(listOf("19:00", "20:00", "21:00"), result.blocks.map { it.startTime })
        assertEquals("Edited", result.blocks[1].title)
        assertEquals(setOf(1), result.checkedIndexes)
    }

    @Test
    fun `replace re-sorts moved block and carries its tick`() {
        val blocks = listOf(block("19:00"), block("20:00"), block("21:00"))
        // Move the ticked middle block (index 1) later — it should land last, still ticked.
        val result = PlanBlockEdits.replace(blocks, setOf(1), 1, block("22:00"))

        assertEquals(listOf("19:00", "21:00", "22:00"), result.blocks.map { it.startTime })
        assertEquals(setOf(2), result.checkedIndexes)
        assertEquals("Block at 22:00", result.blocks[2].title)
    }

    @Test
    fun `replace moving a block earlier shifts other ticks`() {
        val blocks = listOf(block("19:00"), block("20:00"), block("21:00"))
        // Ticks on blocks 1 (20:00) and 2 (21:00); edit 21:00 -> 18:30.
        val result = PlanBlockEdits.replace(blocks, setOf(1, 2), 2, block("18:30"))

        assertEquals(listOf("18:30", "19:00", "20:00"), result.blocks.map { it.startTime })
        // 18:30 (was index 2) lands first — ticked; 19:00 (index 0) untouched —
        // unticked; 20:00 (was index 1) slides to last — still ticked.
        assertEquals(setOf(0, 2), result.checkedIndexes)
    }

    @Test
    fun `replace with out-of-range index is a no-op`() {
        val blocks = listOf(block("19:00"))
        val result = PlanBlockEdits.replace(blocks, setOf(0), 5, block("22:00"))

        assertEquals(blocks, result.blocks)
        assertEquals(setOf(0), result.checkedIndexes)
    }

    // ── remove ─────────────────────────────────────────────────────────────

    @Test
    fun `remove drops the block and its own tick`() {
        val blocks = listOf(block("19:00"), block("20:00"), block("21:00"))
        val result = PlanBlockEdits.remove(blocks, setOf(0, 1), 1)

        assertEquals(listOf("19:00", "21:00"), result.blocks.map { it.startTime })
        assertEquals(setOf(0), result.checkedIndexes)
    }

    @Test
    fun `remove ahead of a ticked block decrements the tick index`() {
        val blocks = listOf(block("19:00"), block("20:00"), block("21:00"))
        // Ticked block 2 (21:00); remove unticked block 0 → tick slides to 1.
        val result = PlanBlockEdits.remove(blocks, setOf(2), 0)

        assertEquals(listOf("20:00", "21:00"), result.blocks.map { it.startTime })
        assertEquals(setOf(1), result.checkedIndexes)
    }

    @Test
    fun `remove with out-of-range index is a no-op`() {
        val blocks = listOf(block("19:00"))
        val result = PlanBlockEdits.remove(blocks, emptySet(), -1)

        assertEquals(blocks, result.blocks)
    }

    // ── insert ─────────────────────────────────────────────────────────────

    @Test
    fun `insert slots by time and shifts later ticks up`() {
        val blocks = listOf(block("19:00"), block("21:00"))
        // Ticked block at index 1 (21:00); insert 20:00 before it → tick becomes 2.
        val result = PlanBlockEdits.insert(blocks, setOf(1), block("20:00"))

        assertEquals(listOf("19:00", "20:00", "21:00"), result.blocks.map { it.startTime })
        assertEquals(setOf(2), result.checkedIndexes)
    }

    @Test
    fun `insert never arrives ticked`() {
        val blocks = listOf(block("19:00"))
        val result = PlanBlockEdits.insert(blocks, setOf(0), block("18:00"))

        assertEquals(listOf("18:00", "19:00"), result.blocks.map { it.startTime })
        // The pre-existing tick follows 19:00 to position 1; the NEW block
        // (position 0) must never inherit a tick.
        assertEquals(setOf(1), result.checkedIndexes)
        assertFalse(0 in result.checkedIndexes)
    }

    @Test
    fun `insert with equal time lands after the existing block`() {
        val blocks = listOf(block("19:00"), block("20:00"))
        val result = PlanBlockEdits.insert(blocks, setOf(1), block("20:00", "Twin"))

        assertEquals(listOf("19:00", "20:00", "20:00"), result.blocks.map { it.startTime })
        assertEquals("Twin", result.blocks[2].title)
        assertEquals(setOf(1), result.checkedIndexes)
    }

    // ── invariants across operations ───────────────────────────────────────

    @Test
    fun `ticks survive a full edit of every block`() {
        var blocks = listOf(block("19:00"), block("20:00"), block("21:00"))
        var ticks = setOf(0, 2) // 19:00 and 21:00 ticked; 20:00 not

        // Each edit targets an index of the CURRENT (re-sorted) list, so edit
        // last-to-first to keep earlier indexes stable while proving remap.
        // 1) 21:00 -> 22:00: stays last, tick stays at 2.
        PlanBlockEdits.replace(blocks, ticks, 2, block("22:00")).let {
            blocks = it.blocks; ticks = it.checkedIndexes
        }
        assertEquals(listOf("19:00", "20:00", "22:00"), blocks.map { it.startTime })
        assertEquals(setOf(0, 2), ticks)

        // 2) ticked 19:00 -> 23:00: slides from first to last; tick 0 -> 2.
        PlanBlockEdits.replace(blocks, ticks, 0, block("23:00")).let {
            blocks = it.blocks; ticks = it.checkedIndexes
        }
        assertEquals(listOf("20:00", "22:00", "23:00"), blocks.map { it.startTime })
        assertEquals(setOf(1, 2), ticks)

        // 3) ticked 22:00 (middle) -> 18:00: jumps to first; tick 1 -> 0.
        PlanBlockEdits.replace(blocks, ticks, 1, block("18:00")).let {
            blocks = it.blocks; ticks = it.checkedIndexes
        }
        assertEquals(listOf("18:00", "20:00", "23:00"), blocks.map { it.startTime })
        assertEquals(setOf(0, 2), ticks)

        assertTrue(ticks.all { it in blocks.indices })
    }

    @Test
    fun `every tick index stays inside blocks bounds after any op`() {
        val blocks = listOf(block("19:00"), block("20:00"))
        val ops = listOf(
            PlanBlockEdits.insert(blocks, setOf(0, 1), block("18:00")),
            PlanBlockEdits.remove(blocks, setOf(0, 1), 0),
            PlanBlockEdits.replace(blocks, setOf(0, 1), 1, block("23:00")),
        )
        ops.forEach { r ->
            assertTrue(r.checkedIndexes.all { it in r.blocks.indices })
        }
    }

    // ── findOverlap ────────────────────────────────────────────────────

    @Test
    fun `findOverlap returns null when ranges only touch back-to-back`() {
        // 20:00–20:30 then 20:30 — adjacent is fine, only real
        // intersections are conflicts.
        val blocks = listOf(block("20:00", duration = 30))
        assertNull(PlanBlockEdits.findOverlap(block("20:30", duration = 10), blocks))
    }

    @Test
    fun `findOverlap detects a partial intersection`() {
        val blocks = listOf(block("20:00", duration = 30)) // 20:00–20:30
        val hit = PlanBlockEdits.findOverlap(block("20:15", duration = 30), blocks) // 20:15–20:45
        assertEquals("20:00", hit?.startTime)
    }

    @Test
    fun `findOverlap detects an identical range and a contained one`() {
        val blocks = listOf(block("20:00", duration = 30))
        assertEquals(
            "20:00",
            PlanBlockEdits.findOverlap(block("20:00", duration = 30), blocks)?.startTime
        )
        assertEquals(
            "20:00",
            PlanBlockEdits.findOverlap(block("20:10", duration = 5), blocks)?.startTime
        )
    }

    @Test
    fun `findOverlap excludes the block being edited`() {
        // Re-saving a block at its own time must never conflict with itself.
        val blocks = listOf(block("20:00", duration = 30))
        assertNull(
            PlanBlockEdits.findOverlap(block("20:00", duration = 30), blocks, excludeIndex = 0)
        )
    }

    @Test
    fun `findOverlap still reports other blocks when one is excluded`() {
        val blocks = listOf(block("19:00", duration = 30), block("21:00", duration = 30))
        val hit = PlanBlockEdits.findOverlap(
            block("21:15", duration = 30),
            blocks,
            excludeIndex = 0,
        )
        assertEquals("21:00", hit?.startTime)
    }

    @Test
    fun `findOverlap never false-positives on unparseable times`() {
        assertNull(PlanBlockEdits.findOverlap(block("banana"), listOf(block("20:00"))))
        assertNull(PlanBlockEdits.findOverlap(block("20:15"), listOf(block("banana"))))
    }

    @Test
    fun `findOverlap clamps at midnight so a late block cannot spill into tomorrow`() {
        // 23:30 + 60 min is treated as ending at midnight — no collision
        // with the early-morning block (documented clamp).
        val blocks = listOf(block("00:15", duration = 30))
        assertNull(PlanBlockEdits.findOverlap(block("23:30", duration = 60), blocks))
    }

    // ── isDue ──────────────────────────────────────────────────────────

    private val today = "2026-09-25"

    /** Epoch millis for a device-local wall time, same rule as PlanExpiryTest. */
    private fun millisAt(date: String, hour: Int, minute: Int): Long =
        java.time.LocalDateTime.parse("${date}T${java.time.LocalTime.of(hour, minute)}")
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    @Test
    fun `isDue is false during the block and true once its duration has elapsed`() {
        val block = block("20:00", duration = 30) // 20:00–20:30
        assertFalse(PlanBlockEdits.isDue(today, block, millisAt(today, 20, 0)))
        assertFalse(PlanBlockEdits.isDue(today, block, millisAt(today, 20, 29)))
        assertTrue(PlanBlockEdits.isDue(today, block, millisAt(today, 20, 30)))
        assertTrue(PlanBlockEdits.isDue(today, block, millisAt(today, 22, 0)))
        // Earlier the same day is also not due.
        assertFalse(PlanBlockEdits.isDue(today, block, millisAt(today, 19, 59)))
    }

    @Test
    fun `isDue is permissive when the time or date cannot be parsed`() {
        // A malformed block must never trap its checkbox forever.
        assertTrue(PlanBlockEdits.isDue(today, block("banana"), millisAt(today, 12, 0)))
        assertTrue(PlanBlockEdits.isDue("not-a-date", block("23:00"), millisAt(today, 12, 0)))
    }

    // ── endTimeHhMm ────────────────────────────────────────────────────

    @Test
    fun `endTimeHhMm returns the plain end and wraps past midnight`() {
        assertEquals("20:30", PlanBlockEdits.endTimeHhMm(block("20:00", duration = 30)))
        assertEquals("01:00", PlanBlockEdits.endTimeHhMm(block("23:00", duration = 120)))
        assertNull(PlanBlockEdits.endTimeHhMm(block("banana")))
    }

    // ── move (drag-reorder) ────────────────────────────────────────────────

    @Test
    fun `move down swaps slots keeping time order and carries the tick`() {
        val blocks = listOf(block("19:00", "A"), block("19:30", "B"), block("20:00", "C"))
        // Drag A below C: identities move, the sorted slot TIMES stay put.
        val result = PlanBlockEdits.move(blocks, setOf(0), from = 0, to = 2)

        assertEquals(listOf("19:00", "19:30", "20:00"), result.blocks.map { it.startTime })
        assertEquals(listOf("B", "C", "A"), result.blocks.map { it.title })
        // A's tick followed A to the end.
        assertEquals(setOf(2), result.checkedIndexes)
    }

    @Test
    fun `move up shifts the blocks in between and their ticks`() {
        val blocks = listOf(block("19:00", "A"), block("19:30", "B"), block("20:00", "C"))
        // Drag C to the top; B and C were ticked.
        val result = PlanBlockEdits.move(blocks, setOf(1, 2), from = 2, to = 0)

        assertEquals(listOf("19:00", "19:30", "20:00"), result.blocks.map { it.startTime })
        assertEquals(listOf("C", "A", "B"), result.blocks.map { it.title })
        // C lands first, B slides to last — both stay ticked; A never was.
        assertEquals(setOf(0, 2), result.checkedIndexes)
    }

    @Test
    fun `move rejects a longer block landing in a shorter slot`() {
        // A runs 60 min; B's slot only has 30 before C starts.
        val blocks = listOf(
            block("19:00", "A", duration = 60),
            block("20:00", "B", duration = 30),
            block("20:30", "C", duration = 30),
        )
        // A into B's slot → 20:00–21:00 would swallow C at 20:30.
        val result = PlanBlockEdits.move(blocks, emptySet(), from = 0, to = 1)

        // Rejected: the very same list instance comes back.
        assertTrue(result.blocks === blocks)
        assertEquals(setOf<Int>(), result.checkedIndexes)
    }

    @Test
    fun `move with equal or out-of-range indexes is a no-op`() {
        val blocks = listOf(block("19:00", "A"), block("19:30", "B"))

        assertTrue(PlanBlockEdits.move(blocks, setOf(0), from = 1, to = 1).blocks === blocks)
        assertTrue(PlanBlockEdits.move(blocks, setOf(0), from = 0, to = 5).blocks === blocks)
        assertTrue(PlanBlockEdits.move(blocks, setOf(0), from = -1, to = 0).blocks === blocks)
    }
}
