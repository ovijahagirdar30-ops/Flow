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

// Fallback cutoff time: if the geofence hasn't already triggered a check-in
// by this time, fire anyway. Not yet backed by a user setting — isolated
// here so a future Settings screen only has to replace this one spot.
private const val DEFAULT_CUTOFF_HOUR = 21 // 9 PM
private const val DEFAULT_CUTOFF_MINUTE = 0

/**
 * Schedules the fallback cutoff alarm — a safety net for the geofence-based
 * check-in trigger. Geofence transitions can lag by minutes to much longer
 * depending on device battery state, and won't fire at all if you don't
 * leave home that day, so this guarantees a check-in still happens by a
 * fixed time if nothing else has.
 *
 * Deliberately targets CheckinAlarmReceiver by its fully-qualified class
 * NAME (a plain string), not a compile-time class reference — core modules
 * shouldn't depend on feature modules, so this is the correct way to reach
 * across that boundary rather than adding an illegal dependency just to
 * write `CheckinAlarmReceiver::class.java`.
 *
 * Daily recurrence is self-rescheduling rather than AlarmManager's
 * setRepeating(), which is inexact by design on modern Android and can
 * drift by many minutes. CheckinAlarmReceiver calls scheduleFallbackCutoff()
 * again every time it fires, to queue up tomorrow's cutoff.
 */
@Singleton
class CheckinAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    /** Computes the next occurrence of the cutoff time: today if it hasn't passed yet, otherwise tomorrow. */
    private fun nextTriggerTimeMillis(): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, DEFAULT_CUTOFF_HOUR)
            set(Calendar.MINUTE, DEFAULT_CUTOFF_MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!next.after(now)) {
            next.add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis
    }

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
     * Schedules (or re-schedules) the next fallback cutoff alarm. Safe to
     * call repeatedly — reuses the same request code, so it overwrites any
     * existing pending alarm rather than stacking duplicates. Call this at
     * app startup and again from CheckinAlarmReceiver itself right after it
     * fires, to queue up the following day.
     */
    fun scheduleFallbackCutoff() {
        if (!canScheduleExactAlarms()) return
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTriggerTimeMillis(),
                checkinPendingIntent()
            )
        } catch (e: SecurityException) {
            // Permission revoked between the check above and this call — fail quietly.
        }
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
}