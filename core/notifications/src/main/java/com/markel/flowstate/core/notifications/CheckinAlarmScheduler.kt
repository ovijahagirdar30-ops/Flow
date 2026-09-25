package com.markel.flowstate.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val CHECKIN_RECEIVER_CLASS = "com.markel.flowstate.feature.checkin.CheckinAlarmReceiver"
private const val CHECKIN_REQUEST_CODE = 9001

/**
 * Alarm plumbing for the check-in pipeline. The 9PM fallback cutoff has
 * been removed — arriving home (GeofenceReceiver) is now the only real
 * trigger — so what remains is:
 *  - cancelFallbackCutoff(): clears any cutoff alarm queued by an older
 *    build, so the removed fallback can't fire one last time;
 *  - scheduleTest(): debug-only manual trigger for the adb test hook.
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
}
