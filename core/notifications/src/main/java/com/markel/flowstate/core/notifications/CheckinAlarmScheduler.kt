package com.markel.flowstate.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

private const val CHECKIN_RECEIVER_CLASS = "com.markel.flowstate.feature.checkin.CheckinAlarmReceiver"
private const val CHECKIN_REQUEST_CODE = 9001
private const val NIGHT_REVIEW_REQUEST_CODE = 9003

/**
 * Intent extra marking an alarm as the nightly9PM night-review page rather
 * than the arrival check-in. Read by CheckinAlarmReceiver.
 */
const val EXTRA_NIGHT_REVIEW = "com.markel.flowstate.extra.NIGHT_REVIEW"/**
 * Alarm plumbing for the check-in pipeline:
 *  - scheduleNightReview(): arms the nightly9PM day-review page for the
 *    next21:00 local (the receiver re-arms it after each fire);
 *  - cancelFallbackCutoff(): clears any cutoff alarm queued by an older
 *    build, so the removed arrival fallback can't fire one last time;
 *  - scheduleTest() / scheduleNightReviewInTest(): debug-only manual
 *    triggers for the adb test hooks.
 *
 * Deliberately targets CheckinAlarmReceiver by its fully-qualified class
 * NAME (a plain string), not a compile-time class reference — core modules
 * shouldn't depend on feature modules, so this is the correct way to reach
 * across that boundary rather than adding an illegal dependency just to
 * write `CheckinAlarmReceiver::class.java`.
 */
@Singleton
class CheckinAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    private fun checkinPendingIntent(): PendingIntent {
        val intent = Intent().apply {
            component = ComponentName(context.packageName, CHECKIN_RECEIVER_CLASS)
        }
        return PendingIntent.getBroadcast(
            context,
            CHECKIN_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Cancels any pending check-in cutoff alarm. Called at app startup so a
     * 9PM alarm scheduled by an older build can't fire after the fallback
     * was removed. No-op when nothing is scheduled. Shares the request code
     * with scheduleTest(), so it would also cancel a debug test alarm still
     * inside its 10-second window — a window that only exists while
     * manually testing.
     */
    fun cancelFallbackCutoff() {
        alarmManager.cancel(checkinPendingIntent())
    }

    /**
     * Schedules a one-off check-in alarm [secondsFromNow] seconds out — for
     * manual testing only. Resets today's debounce flag first, so repeated
     * manual tests on the same day aren't silently swallowed by
     * CheckinTrigger's once-per-day guard.
     */
    fun scheduleTest(secondsFromNow: Long) {
        if (!canScheduleExactAlarms()) return
        CheckinDebounce.resetForTesting(context)
        val triggerAtMillis = System.currentTimeMillis() + secondsFromNow * 1000
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, checkinPendingIntent())
        } catch (e: SecurityException) {
            // Permission revoked between the check above and this call — fail quietly.
        }
    }

    /**
     * Arms the nightly9PM night-review page for the next occurrence of
     * 21:00 local time — today if it hasn't passed yet, otherwise tomorrow.
     * Safe to call repeatedly: the same request code overwrites the pending
     * alarm instead of stacking duplicates. CheckinAlarmReceiver re-arms
     * after each fire, so once scheduled it repeats daily until reboot
     * (every app launch arms it again).
     *
     * Unlike the old arrival fallback, a withheld exact-alarm permission
     * does NOT disable it — it degrades to an inexact alarm so the page
     * still shows up (possibly a few minutes late).
     */
    fun scheduleNightReview() {
        scheduleNightReviewAt(nextNightReviewMillis())
    }

    /** Schedules the night review [secondsFromNow] seconds out — for manual adb testing. */
    fun scheduleNightReviewInTest(secondsFromNow: Long) {
        scheduleNightReviewAt(System.currentTimeMillis() + secondsFromNow * 1000)
    }

    private fun scheduleNightReviewAt(triggerAtMillis: Long) {
        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, nightReviewPendingIntent()
                )
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, nightReviewPendingIntent())
            }
        } catch (e: SecurityException) {
            // Permission revoked between the check above and this call — fail quietly.
        }
    }

    /** Next21:00 local: today if it's still ahead, otherwise tomorrow. */
    private fun nextNightReviewMillis(): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!next.after(now)) {
            next.add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis
    }

    /** Distinct request code + NIGHT_REVIEW extra, so it never collides with the check-in/test alarms. */
    private fun nightReviewPendingIntent(): PendingIntent {
        val intent = Intent().apply {
            component = ComponentName(context.packageName, CHECKIN_RECEIVER_CLASS)
            putExtra(EXTRA_NIGHT_REVIEW, true)
        }
        return PendingIntent.getBroadcast(
            context,
            NIGHT_REVIEW_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
