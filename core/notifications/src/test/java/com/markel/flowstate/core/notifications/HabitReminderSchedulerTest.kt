package com.markel.flowstate.core.notifications

import com.markel.flowstate.core.domain.Habit
import com.markel.flowstate.core.domain.HabitEntryFlat
import com.markel.flowstate.core.domain.HabitRepository
import com.markel.flowstate.core.domain.HabitSchedule
import com.markel.flowstate.core.domain.HabitType
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Books one alarm per habit and re-arms it when it fires — the wiring between
 * [HabitReminderPlanner]'s rules and the exact-alarm scheduler.
 */
class HabitReminderSchedulerTest {

    private val reminderScheduler: ReminderScheduler = mockk(relaxed = true)
    private val habitRepository: HabitRepository = mockk()
    private val scheduler = HabitReminderScheduler(reminderScheduler, habitRepository)

    private val now = System.currentTimeMillis()
    private val today = LocalDate.now()

    private fun habit(
        id: Int = 1,
        name: String = "Meditation",
        enabled: Boolean = true,
        minuteOfDay: Int = 8 * 60,
        schedule: HabitSchedule = HabitSchedule.DAILY,
        habitType: HabitType = HabitType.BOOLEAN,
        targetValue: Float? = null,
    ) = Habit(
        id = id,
        name = name,
        schedule = schedule,
        habitType = habitType,
        targetValue = targetValue,
        reminderEnabled = enabled,
        reminderMinuteOfDay = minuteOfDay
    )

    private fun stub(habit: Habit?, booleanEntries: List<HabitEntryFlat> = emptyList()) {
        coEvery { habitRepository.getHabitById(any()) } returns habit
        coEvery { habitRepository.getAllEntries() } returns flowOf(booleanEntries)
        coEvery { habitRepository.getAllNumericEntries() } returns flowOf(emptyList())
    }

    private val dueToday = HabitEntryFlat(habitId = 1, epochDay = today.toEpochDay())

    // ── reschedule ───────────────────────────────────────────────────────────

    @Test
    fun reschedule_booksAFutureAlarm_forAnEnabledHabit() = runTest {
        stub(habit())

        scheduler.reschedule(1, now)

        verify(exactly = 1) { reminderScheduler.scheduleHabit(1, "Meditation", match { it > now }) }
        verify(exactly = 0) { reminderScheduler.cancelHabit(any()) }
    }

    @Test
    fun reschedule_cancels_whenReminderIsOff() = runTest {
        stub(habit(enabled = false))

        scheduler.reschedule(1, now)

        verify(exactly = 1) { reminderScheduler.cancelHabit(1) }
        verify(exactly = 0) { reminderScheduler.scheduleHabit(any(), any(), any()) }
    }

    @Test
    fun reschedule_cancels_whenHabitWasDeleted() = runTest {
        stub(null)

        scheduler.reschedule(1, now)

        verify(exactly = 1) { reminderScheduler.cancelHabit(1) }
    }

    @Test
    fun reschedule_completedToday_stillBooksTomorrow() = runTest {
        stub(habit(), booleanEntries = listOf(dueToday))

        scheduler.reschedule(1, now)

        verify(exactly = 1) {
            reminderScheduler.scheduleHabit(1, "Meditation", match { trigger ->
                trigger > now &&
                    java.time.Instant.ofEpochMilli(trigger).atZone(java.time.ZoneId.systemDefault()).toLocalDate() > today
            })
        }
    }

    @Test
    fun rescheduleAll_booksEveryEnabledHabit_andSkipsDisabledOnes() = runTest {
        val enabled = habit(id = 1)
        val disabled = habit(id = 2, name = "Stretching", enabled = false)
        coEvery { habitRepository.getHabits() } returns flowOf(listOf(enabled, disabled))
        coEvery { habitRepository.getAllEntries() } returns flowOf(emptyList())
        coEvery { habitRepository.getAllNumericEntries() } returns flowOf(emptyList())

        scheduler.rescheduleAll(now)

        verify(exactly = 1) { reminderScheduler.scheduleHabit(1, "Meditation", any()) }
        verify(exactly = 1) { reminderScheduler.cancelHabit(2) }
    }

    @Test
    fun rescheduleAll_doesNothing_whenThereAreNoHabits() = runTest {
        coEvery { habitRepository.getHabits() } returns flowOf(emptyList())

        scheduler.rescheduleAll(now)

        verify(exactly = 0) { reminderScheduler.scheduleHabit(any(), any(), any()) }
        verify(exactly = 0) { reminderScheduler.cancelHabit(any()) }
    }

    // ── consumeAlarm (the alarm just fired) ──────────────────────────────────

    @Test
    fun consumeAlarm_dueHabit_returnsNoticeAndReArms() = runTest {
        stub(habit())

        val notice = scheduler.consumeAlarm(1, now)

        assertNotNull(notice)
        assertEquals(ReminderScheduler.habitRequestCode(1), notice!!.notificationId)
        assertEquals("Meditation", notice.title)
        // Today's slot is spent → the next occurrence is booked.
        verify(exactly = 1) { reminderScheduler.scheduleHabit(1, "Meditation", match { it > now }) }
    }

    @Test
    fun consumeAlarm_alreadyDoneToday_staysSilentAndStillReArms() = runTest {
        stub(habit(), booleanEntries = listOf(dueToday))

        assertNull(scheduler.consumeAlarm(1, now))

        verify(exactly = 1) { reminderScheduler.scheduleHabit(1, "Meditation", match { it > now }) }
        verify(exactly = 0) { reminderScheduler.cancelHabit(any()) }
    }

    @Test
    fun consumeAlarm_offDay_staysSilentButStillReArms() = runTest {
        // Every weekday EXCEPT today → today is an off-day by construction,
        // whatever the calendar says when the test runs.
        val otherDays = DayOfWeek.entries.filter { it != today.dayOfWeek }.toSet()
        stub(habit(schedule = HabitSchedule(otherDays)))

        assertNull(scheduler.consumeAlarm(1, now))

        // Tomorrow is always a different weekday → the chain still moves on.
        verify(exactly = 1) { reminderScheduler.scheduleHabit(1, "Meditation", match { it > now }) }
    }

    @Test
    fun consumeAlarm_disabledHabit_cancelsAndStaysSilent() = runTest {
        stub(habit(enabled = false))

        assertNull(scheduler.consumeAlarm(1, now))

        verify(exactly = 1) { reminderScheduler.cancelHabit(1) }
        verify(exactly = 0) { reminderScheduler.scheduleHabit(any(), any(), any()) }
    }

    @Test
    fun consumeAlarm_deletedHabit_cancelsAndStaysSilent() = runTest {
        stub(null)

        assertNull(scheduler.consumeAlarm(1, now))

        verify(exactly = 1) { reminderScheduler.cancelHabit(1) }
    }

    // ── numeric habits ───────────────────────────────────────────────────────

    @Test
    fun consumeAlarm_numericHabitOverTarget_countsAsCompleted() = runTest {
        val habit = habit(habitType = HabitType.NUMERIC, targetValue = 2f)
        coEvery { habitRepository.getHabitById(any()) } returns habit
        coEvery { habitRepository.getAllEntries() } returns flowOf(emptyList())
        coEvery { habitRepository.getAllNumericEntries() } returns flowOf(
            listOf(com.markel.flowstate.core.domain.HabitNumericEntry(1, today, 3f))
        )

        assertNull(scheduler.consumeAlarm(1, now))
    }

    @Test
    fun consumeAlarm_numericHabitUnderTarget_stillNotifies() = runTest {
        val habit = habit(habitType = HabitType.NUMERIC, targetValue = 5f)
        coEvery { habitRepository.getHabitById(any()) } returns habit
        coEvery { habitRepository.getAllEntries() } returns flowOf(emptyList())
        coEvery { habitRepository.getAllNumericEntries() } returns flowOf(
            listOf(com.markel.flowstate.core.domain.HabitNumericEntry(1, today, 2f))
        )

        val notice = scheduler.consumeAlarm(1, now)

        assertNotNull(notice)
        assertEquals(ReminderScheduler.habitRequestCode(1), notice!!.notificationId)
    }
}
