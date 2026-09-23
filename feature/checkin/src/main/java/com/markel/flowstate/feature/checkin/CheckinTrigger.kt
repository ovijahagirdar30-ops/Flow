package com.markel.flowstate.feature.checkin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.markel.flowstate.core.notifications.CheckinDebounce

/**
 * Shared "fire the check-in" logic used by both trigger paths:
 * CheckinAlarmReceiver (fallback / manual test) and GeofenceReceiver
 * (arrival-at-home, the primary trigger). Keeping this in one place means
 * both paths behave identically and only ever need to be fixed once —
 * including the once-per-day debounce below.
 */
object CheckinTrigger {
    private const val TAG = "CheckinTrigger"
    private const val CHANNEL_ID = "checkin_channel"
    private const val NOTIFICATION_ID = 9002

    fun fire(context: Context) {
        if (CheckinDebounce.hasCheckedInToday(context)) {
            Log.i(TAG, "fire() skipped — already checked in today")
            return
        }
        CheckinDebounce.markCheckedInToday(context)
        Log.i(TAG, "fire() — triggering check-in")

        createNotificationChannel(context)
        val fullScreenIntent = Intent(context, CheckinActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        if (Settings.canDrawOverlays(context)) {
            Log.i(TAG, "starting CheckinActivity directly (overlay permission granted)")
            context.startActivity(fullScreenIntent)
        }
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
        Log.i(TAG, "check-in notification posted with full-screen intent")
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
}