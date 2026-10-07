package com.markel.flowstate.core.data

import com.markel.flowstate.core.data.local.HabitDao
import com.markel.flowstate.core.data.local.HabitEntryFlatEntity
import com.markel.flowstate.core.data.local.HabitNumericEntryEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * The numeric-habit mood fix: a rating must land somewhere readable no
 * matter which table holds the day's row, must show up merged in the
 * habit's mood history, and must survive later edits to the day's value.
 */
class HabitRepositoryImplTest {

    private val dao: HabitDao = mockk(relaxed = true)
    private val repository = HabitRepositoryImpl(dao)
    private val date = LocalDate.of(2026, 10, 7)

    /** setEntryMood writes BOTH day tables — the one without the row no-ops. */
    @Test
    fun setEntryMood_writesBothDayTables() = runTest {
        repository.setEntryMood(3, date, 5)

        val epochDay = date.toEpochDay()
        coVerify(exactly = 1) { dao.setMood(3, epochDay, 5) }
        coVerify(exactly = 1) { dao.setNumericMood(3, epochDay, 5) }
    }

    /** The habit's mood history must merge boolean + numeric ratings, newest first. */
    @Test
    fun getMoodsForHabit_mergesBooleanAndNumericRatings_newestFirst() = runTest {
        coEvery { dao.getMoodsForHabit(3) } returns flowOf(
            listOf(HabitEntryFlatEntity(habitId = 3, epochDay = 100, mood = 4))
        )
        coEvery { dao.getNumericMoodsForHabit(3) } returns flowOf(
            listOf(
                HabitEntryFlatEntity(habitId = 3, epochDay = 200, mood = 2),
                HabitEntryFlatEntity(habitId = 3, epochDay = 50, mood = 5)
            )
        )

        val moods = repository.getMoodsForHabit(3).first()

        assertEquals(listOf(200L, 100L, 50L), moods.map { it.epochDay })
        assertEquals(listOf(2, 4, 5), moods.map { it.mood })
    }

    /** REPLACE-inserting a new value must not erase the day's rating. */
    @Test
    fun logNumericEntry_carriesExistingMoodAcrossTheReplaceUpsert() = runTest {
        coEvery { dao.getNumericEntryOnce(3, date.toEpochDay()) } returns
            HabitNumericEntryEntity(habitId = 3, epochDay = date.toEpochDay(), value = 1f, mood = 4)

        repository.logNumericEntry(3, date, 2f)

        coVerify {
            dao.upsertNumericEntry(match { it.value == 2f && it.mood == 4 })
        }
    }

    /** A first-ever value for the day stores a null mood, not a stale one. */
    @Test
    fun logNumericEntry_withoutExistingRow_storesNullMood() = runTest {
        coEvery { dao.getNumericEntryOnce(3, date.toEpochDay()) } returns null

        repository.logNumericEntry(3, date, 1f)

        coVerify { dao.upsertNumericEntry(match { it.mood == null }) }
    }
}
