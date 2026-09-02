package com.markel.flowstate.feature.checkin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat

/**
 * Ported from the standalone Phase 0 test project, where this exact
 * approach was confirmed working on a real device: interrupts the user
 * regardless of lock state.
 */
class CheckinAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        createNotificationChannel(context)

        val fullScreenIntent = Intent(context, CheckinActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // Primary path: launches directly over whatever the user is doing,
        // even while the phone is unlocked — requires "display over other
        // apps" (SYSTEM_ALERT_WINDOW), one of the exceptions Android allows
        // for starting an activity from the background.
        if (Settings.canDrawOverlays(context)) {
            context.startActivity(fullScreenIntent)
        }

        // Backup path: makes it appear over the lock screen specifically,
        // and covers the case where the overlay permission isn't granted.
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // placeholder icon
            .setContentTitle("Check-in time")
            .setContentText("Tap to see today's tasks and habits.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Check-in alarms",
            NotificationManager.IMPORTANCE_HIGH // must be HIGH for full-screen to work
        ).apply {
            description = "Triggers the evening check-in screen"
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "checkin_channel"
        const val NOTIFICATION_ID = 9002
    }
}