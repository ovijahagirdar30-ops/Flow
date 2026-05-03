package com.markel.flowstate.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.markel.flowstate.core.domain.TaskRepository
import com.markel.flowstate.core.domain.usecase.tasks.ToggleTaskUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CompleteTaskReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    @Inject
    lateinit var toggleTaskUseCase: ToggleTaskUseCase

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if (taskId == -1) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                val task = taskRepository.getTaskById(taskId)
                if (task != null && !task.isDone) {
                    toggleTaskUseCase(task)

                    // Cancel alarm and subtask alarms
                    reminderScheduler.cancel(taskId)
                    task.subTasks.filter { it.reminderTime != null }.forEach { subTask ->
                        reminderScheduler.cancelSubTask(subTask.id)
                    }
                }

                // Discard notification
                NotificationManagerCompat.from(context).cancel(taskId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}