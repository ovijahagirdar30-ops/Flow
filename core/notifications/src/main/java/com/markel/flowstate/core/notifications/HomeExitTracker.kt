package com.markel.flowstate.core.notifications

import android.content.Context

/**
 * Records when the device was last SEEN leaving the home geofence, so the
 * ENTER side of the fence can be gated on an actual departure instead of
 * blindly trusting every ENTER Play Services delivers.
 *
 * Why this exists: GMS is known to emit phantom ENTERs — the first location
 * fix after a doze window, or accuracy noise wider than the 150m fence, can
 * be reported as an outside->inside transition while the phone never moved.
 * One landed at 00:24 while the user was asleep; worse, because
 * CheckinDebounce rolls over at midnight, it silently stole that day's REAL
 * arrival check-in. An ENTER is only real if a qualifying EXIT preceded it.
 *
 * Deliberately mirrors CheckinDebounce's shape (tiny object, dedicated
 * SharedPreferences file) so a future "how long was the user away today"
 * feature — useful context for the AI brain — can read the same record
 * without needing a new store.
 */
object HomeExitTracker {
    private const val PREFS_NAME = "home_exit"
    private const val KEY_LAST_EXIT_TIME = "last_exit_time_millis"

    /** Called by GeofenceReceiver on every GEOFENCE_TRANSITION_EXIT. */
    fun markExited(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_EXIT_TIME, System.currentTimeMillis()).apply()
    }

    /**
     * Milliseconds since the last recorded exit, or null if the device has
     * never been observed leaving (fresh install, cleared app data). Callers
     * decide what null means: GeofenceReceiver treats it as trust-first
     * (allow the ENTER — quiet hours still apply) so a fresh install never
     * swallows the user's real first arrival.
     */
    fun millisSinceLastExit(context: Context): Long? {
        val lastExit = prefs(context).getLong(KEY_LAST_EXIT_TIME, -1L)
        return if (lastExit < 0L) null else System.currentTimeMillis() - lastExit
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
