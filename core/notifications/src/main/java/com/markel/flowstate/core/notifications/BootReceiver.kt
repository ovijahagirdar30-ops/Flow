package com.markel.flowstate.core.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.markel.flowstate.core.domain.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives BOOT_COMPLETED and reprograms all pending reminders.
 *
 * AlarmManager alarms are wiped on device reboot. This receiver queries the DB
 * for every task that still has a reminderTime in the future and reschedules them.
 * The missed notifications are fired immediately and the reminderTime is cleaned.
 *
 * Note: BroadcastReceiver.onReceive must return quickly. We launch a coroutine
 * with goAsync() to do the DB work without blocking the main thread.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var notificationHelper: NotificationHelper

    // A dedicated scope that outlives onReceive (goAsync result is kept alive).
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"  // HTC devices
        ) return

        val pendingResult = goAsync()

        scope.launch {
            try {
                val now = System.currentTimeMillis()
                val alarmItems = buildAlarmItems(taskRepository)

                // Separate future alarms (reschedule after reboot) and missed alarms (fire them now)
                val futureAlarms = alarmItems.filter { it.triggerMillis > now }
                val expiredAlarms = alarmItems.filter { it.triggerMillis <= now }

                // Reschedule
                reminderScheduler.rescheduleAll(futureAlarms)

                // Fire immediately expired alarmas
                expiredAlarms.forEach { item ->
                    showExpiredNotification(context, item)
                    // Consume the reminder (clean in the DB)
                    if (item.isSubtask && item.subTaskId != null) {
                        taskRepository.clearSubTaskReminder(item.subTaskId)
                    } else {
                        taskRepository.clearTaskReminder(item.requestCode)
                    }
                }

            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Fires a notification for an alarm that expired while the device was off
     */
    private fun showExpiredNotification(context: Context, item: AlarmItem) {
        val completeIntent = Intent(context, CompleteTaskReceiver::class.java).apply {
            putExtra(CompleteTaskReceiver.EXTRA_TASK_ID, item.requestCode)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            item.requestCode,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notificationHelper.showReminder(
            notificationId = item.requestCode,
            taskTitle = item.title,
            taskDescription = item.description,
            completePendingIntent = completePendingIntent
        )
    }
}

/**
 * Represents a single alarm to be scheduled.
 * Used by [buildAlarmItems], [ReminderScheduler.rescheduleAll], and related code.
 */
data class AlarmItem(
    val requestCode: Int,
    val title: String,
    val description: String? = null,
    val triggerMillis: Long,
    val isSubtask: Boolean = false,
    val subTaskId: String? = null
)

/**
 * Builds the flat list of [AlarmItem] for all tasks and subtasks that have a
 * future reminder. Shared by BootReceiver and FlowViewModel so the rescheduling
 * logic stays in one place.
 */
suspend fun buildAlarmItems(taskRepository: TaskRepository): List<AlarmItem> {
    val result = mutableListOf<AlarmItem>()

    taskRepository.getTasks().first().forEach { task ->
        if (task.isDone) return@forEach
        task.reminderTime?.let {
            result += AlarmItem(
                requestCode = task.id,
                title = task.title,
                description = task.description,
                triggerMillis = it,
                isSubtask = false,
                subTaskId = null
            )
        }
        task.subTasks.forEach { sub ->
            if (sub.isDone) return@forEach
            sub.reminderTime?.let {
                result += AlarmItem(
                    requestCode = sub.id.hashCode(),
                    title = sub.title,
                    triggerMillis = it,
                    isSubtask = true,
                    subTaskId = sub.id
                )
            }
        }
    }
    return result
}

