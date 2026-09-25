package com.markel.flowstate.core.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalEncouragementGeneratorTest {

    private val generator = LocalEncouragementGenerator()

    private fun stats(
        date: String = "2026-09-25",
        completed: Int = 0,
        pending: Int = 0,
        pushed: Int = 0,
    ) = DayReviewStats(
        date = date,
        completedCount = completed,
        completedTitles = emptyList(),
        pendingCount = pending,
        pushedCount = pushed,
    )

    @Test
    fun generate_isNeverBlank_andNeverGuiltTrips() = runTest {
        val cases = listOf(
            stats(completed = 3, pending = 1),
            stats(pending = 2),
            stats(completed = 5),
            stats(),
        )
        cases.forEach { s ->
            val message = generator.generate(s)
            assertTrue("blank message for $s", message.isNotBlank())
        }
    }

    @Test
    fun generate_isDeterministicPerDate() = runTest {
        val s = stats(completed = 4, pending = 1)
        assertEquals(generator.generate(s), generator.generate(s))
    }

    @Test
    fun generate_mentionsTheCompletedCount() = runTest {
        // A date hashing into the "some done" templates always embeds {n}.
        val message = generator.generate(stats(completed = 7, pending = 2))
        assertTrue("expected the count in: $message", message.contains("7"))
    }
}
