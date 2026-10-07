package com.markel.flowstate.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.markel.flowstate.core.notifications.R

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "task_reminders"
        private const val CHANNEL_NAME = "Task Reminders"
        private const val CHANNEL_DESCRIPTION = "Notifications for scheduled task reminders"

        // Habit reminders get their own channel so the user can silence one
        // without losing the other in system settings.
        const val HABIT_CHANNEL_ID = "habit_reminders"
        private const val HABIT_CHANNEL_NAME = "Habit Reminders"
        private const val HABIT_CHANNEL_DESCRIPTION = "Notifications for habits you asked to be reminded about"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannel()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)

        val habitChannel = NotificationChannel(
            HABIT_CHANNEL_ID,
            HABIT_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = HABIT_CHANNEL_DESCRIPTION
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(habitChannel)
    }

    /**
     * Shows a reminder notification with a "Complete" action.
     * Tapping the notification opens MainActivity; tapping "Complete" marks the task as done.
     * [notificationId] should be the task ID so each task has its own notification slot
     * (a new reminder for the same task replaces the old one automatically).
     */
    fun showReminder(
        notificationId: Int,
        taskTitle: String,
        taskDescription: String?,
        completePendingIntent: PendingIntent
    ) {
        val tapIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }

        val tapPendingIntent = tapIntent?.let {
            PendingIntent.getActivity(
                context,
                notificationId,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.new_ic_launcher_foreground_white)
            .setContentTitle(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)

        // Show description if exists
        if (!taskDescription.isNullOrBlank()) {
            builder.setContentText(taskDescription)
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(taskDescription))
        }

        // Add action to complete the task
        builder.addAction(
            R.drawable.check_24px,
            context.getString(R.string.notification_action_complete),
            completePendingIntent
        )

        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * Shows a habit reminder — no "Complete" action, because a habit has no
     * single tap-to-done (numeric habits log a value, booleans tick from the
     * card), so tapping just opens the app. [notificationId] is the habit's
     * alarm requestCode, giving each habit its own notification slot.
     */
    fun showHabitReminder(notificationId: Int, habitName: String) {
        val tapIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }

        val tapPendingIntent = tapIntent?.let {
            PendingIntent.getActivity(
                context,
                notificationId,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(context, HABIT_CHANNEL_ID)
            .setSmallIcon(R.drawable.new_ic_launcher_foreground_white)
            .setContentTitle(habitName)
            .setContentText(context.getString(R.string.habit_reminder_body))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.habit_reminder_body)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
