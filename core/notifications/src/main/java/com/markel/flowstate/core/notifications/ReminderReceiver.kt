package com.markel.flowstate.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.markel.flowstate.core.domain.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives the exact alarm fired by AlarmManager and shows the notification.
 * After the notification is shown, the reminder is "consumed" — its reminderTime
 * is cleared in the database so it no longer appears as active in the UI.
 *
 * Annotated with @AndroidEntryPoint so Hilt can inject dependencies.
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_IS_SUBTASK = "extra_is_subtask"
        const val EXTRA_SUBTASK_ID = "extra_subtask_id"

    }

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var taskRepository: TaskRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: return
        if (taskId == -1) return

        // Show the notification
        notificationHelper.showReminder(
            notificationId = taskId,
            taskTitle = taskTitle
        )


        // Consume the reminder — clear reminderTime so it disappears from the UI
        val isSubtask = intent.getBooleanExtra(EXTRA_IS_SUBTASK, false)
        val subTaskId = intent.getStringExtra(EXTRA_SUBTASK_ID)

        val pendingResult = goAsync()
        scope.launch {
            try {
                if (isSubtask && subTaskId != null) {
                    taskRepository.clearSubTaskReminder(subTaskId)
                } else {
                    taskRepository.clearTaskReminder(taskId)
                }
            } finally {
                pendingResult.finish()
            }
        }

    }
}
