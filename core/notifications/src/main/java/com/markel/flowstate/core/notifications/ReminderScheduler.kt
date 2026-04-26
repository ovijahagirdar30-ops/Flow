package com.markel.flowstate.core.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and cancels exact alarms for task reminders.
 *
 * Permission check is handled internally via canScheduleExactAlarms() — callers
 * do not need @RequiresPermission annotations. The UI is responsible for
 * directing the user to system settings if the permission is missing.
 *
 * The alarm survives app process death because it lives in AlarmManager.
 * It does NOT survive a device reboot by itself — BootReceiver handles that.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // ── Tasks ─────────────────────────────────────────────────────────────────

    fun schedule(taskId: Int, taskTitle: String, triggerAtMillis: Long) {
        if (!canScheduleExactAlarms()) return
        setAlarm(
            requestCode = taskId,
            title = taskTitle,
            triggerAtMillis = triggerAtMillis,
            isSubtask = false,
            subTaskId = null
        )
    }

    fun cancel(taskId: Int) {
        cancelAlarm(requestCode = taskId)
    }

    // ── SubTasks ──────────────────────────────────────────────────────────────
    /*
    * SubTask IDs are String UUIDs, so we derive a stable Int requestCode viahashCode().
    * Collision probability across a personal task list is negligible, but the code is
    * defensive: a collision would only misfire one other alarm, not crash the app.
    */

    fun scheduleSubTask(subTaskId: String, subTaskTitle: String, triggerAtMillis: Long) {
        if (!canScheduleExactAlarms()) return
        setAlarm(
            requestCode = subTaskId.hashCode(),
            title = subTaskTitle,
            triggerAtMillis = triggerAtMillis,
            isSubtask = true,
            subTaskId = subTaskId
        )
    }

    fun cancelSubTask(subTaskId: String) {
        cancelAlarm(requestCode = subTaskId.hashCode())
    }

    // ── Bulk reschedule (called from FlowViewModel.onResume) ──────────────────

    /**
     * Reschedules every alarm in [items] whose trigger is in the future.
     * Used when the exact-alarm permission is granted after reminders were already saved,
     * or after a device reboot (via BootReceiver).
     * Each [AlarmItem] carries whether it is a subtask, so the resulting PendingIntent
     * includes the correct extras for the ReminderReceiver to consume the reminder properly.
     */
    fun rescheduleAll(items: List<AlarmItem>) {
        if (!canScheduleExactAlarms()) return
        val now = System.currentTimeMillis()
        items.forEach { item ->
            if (item.triggerMillis > now) setAlarm(
                requestCode = item.requestCode,
                title = item.title,
                triggerAtMillis = item.triggerMillis,
                isSubtask = item.isSubtask,
                subTaskId = item.subTaskId
            )
        }
    }

    // ── Permission ────────────────────────────────────────────────────────────

    /**
     * Returns true if the device can schedule exact alarms.
     * Always true below Android 12 (maybe this if is not needed since the minSDK is 31, but in any case I prefer to check)
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms()
        else true
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun setAlarm(
        requestCode: Int,
        title: String,
        triggerAtMillis: Long,
        isSubtask: Boolean = false,
        subTaskId: String? = null
    ) {
        pendingIntent(requestCode, title, PendingIntent.FLAG_UPDATE_CURRENT, isSubtask, subTaskId)?.let { pi ->
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pi
            )
        }
    }

    private fun cancelAlarm(requestCode: Int) {
        val pi = pendingIntent(requestCode, "", PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(pi)
        pi.cancel()
    }

    private fun pendingIntent(
        requestCode: Int,
        title: String,
        flags: Int,
        isSubtask: Boolean = false,
        subTaskId: String? = null
    ): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ReminderReceiver::class.java).apply {
                putExtra(ReminderReceiver.EXTRA_TASK_ID, requestCode)
                putExtra(ReminderReceiver.EXTRA_TASK_TITLE, title)
                putExtra(ReminderReceiver.EXTRA_IS_SUBTASK, isSubtask)
                subTaskId?.let { putExtra(ReminderReceiver.EXTRA_SUBTASK_ID, it) }
            },
            flags or PendingIntent.FLAG_IMMUTABLE
        )
}
