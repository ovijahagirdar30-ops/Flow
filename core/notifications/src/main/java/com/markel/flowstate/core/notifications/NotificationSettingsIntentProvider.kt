package com.markel.flowstate.core.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the intent to open the Android system notification settings
 * screen specific to this application.
 *
 * Starting from Android 13 (API 33), POST_NOTIFICATIONS is a runtime permission.
 * This class does NOT request the permission dialog — instead, it redirects
 * the user to the system settings where they can grant/revoke it manually,
 * along with managing channel-specific preferences (sound, vibration, etc.).
 */
@Singleton
class NotificationSettingsIntentProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Creates an intent that opens the system notification settings
     * for this application.
     *
     * Uses [Settings.ACTION_APP_NOTIFICATION_SETTINGS] with
     * [Settings.EXTRA_APP_PACKAGE] to target the app's own package.
     *
     * @return the configured [Intent], or null if it cannot be resolved
     *         (should not happen on API 31+, but defensive coding).
     */
    fun createIntent(): Intent? {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

            // Prevent the settings activity from being added to the recents stack
            // as a separate task — it should feel like a temporary departure.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // Verify that the intent can be resolved before returning it.
        // On API 31+ this should always resolve, but we guard against
        // OEM ROMs that might not properly implement this settings action.
        return intent.takeIf {
            it.resolveActivity(context.packageManager) != null
        }
    }

    /**
     * Whether the app currently has the POST_NOTIFICATIONS permission
     * granted. This is only relevant on Android 13+ (API 33).
     *
     * Useful for displaying the current notification permission status
     * in the settings UI (e.g., a summary text under the settings item).
     *
     * On API < 33, notifications are always permitted at install time,
     * so this always returns true.
     */
    fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}