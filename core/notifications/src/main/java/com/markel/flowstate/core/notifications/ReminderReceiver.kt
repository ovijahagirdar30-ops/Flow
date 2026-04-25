package com.markel.flowstate.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receives the exact alarm fired by AlarmManager and shows the notification.
 * Annotated with @AndroidEntryPoint so Hilt can inject NotificationHelper.
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
    }

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: return
        if (taskId == -1) return

        notificationHelper.showReminder(
            notificationId = taskId,
            taskTitle = taskTitle
        )
    }
}
