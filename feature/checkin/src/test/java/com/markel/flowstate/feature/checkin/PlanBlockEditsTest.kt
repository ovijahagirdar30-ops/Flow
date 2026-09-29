package com.markel.flowstate.feature.checkin

import com.markel.flowstate.core.domain.PlanBlock
import com.markel.flowstate.core.domain.PlanBlockKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanBlockEditsTest {

    private fun block(time: String, title: String = "Block at $time") = PlanBlock(
        startTime = time,
        durationMinutes = 30,
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
}
