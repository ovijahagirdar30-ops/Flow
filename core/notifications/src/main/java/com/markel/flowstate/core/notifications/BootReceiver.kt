package com.markel.flowstate.core.notifications

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
 *
 * Note: BroadcastReceiver.onReceive must return quickly. We launch a coroutine
 * with goAsync() to do the DB work without blocking the main thread.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler

    // A dedicated scope that outlives onReceive (goAsync result is kept alive).
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"  // HTC devices
        ) return

        val pendingResult = goAsync()

        scope.launch {
            try {
                val alarmItems = buildAlarmItems(taskRepository)
                reminderScheduler.rescheduleAll(alarmItems)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * Builds the flat list of (requestCode, title, triggerMillis) for all tasks
 * and subtasks that have a future reminder. Shared by BootReceiver and
 * FlowViewModel so the rescheduling logic stays in one place.
 */
suspend fun buildAlarmItems(taskRepository: TaskRepository): List<Triple<Int, String, Long>> {
    val now = System.currentTimeMillis()
    val result = mutableListOf<Triple<Int, String, Long>>()

    taskRepository.getTasks().first().forEach { task ->
        task.reminderTime?.let { if (it > now) result += Triple(task.id, task.title, it) }
        task.subTasks.forEach { sub ->
            sub.reminderTime?.let {
                if (it > now) result += Triple(sub.id.hashCode(), sub.title, it)
            }
        }
    }
    return result
}

