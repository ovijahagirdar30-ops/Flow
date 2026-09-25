package com.markel.flowstate.feature.checkin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.markel.flowstate.core.notifications.HomeExitTracker
import java.util.Calendar

private const val TAG = "GeofenceReceiver"

// No check-in may be forced on the user during the night window — arriving
// home is the only trigger, and a phantom enter here would light the screen
// in a sleeping user's face (the exact bug this guards).
private const val QUIET_HOURS_START = 0   // 00:00
private const val QUIET_HOURS_END = 6     // until 06:00

// An ENTER only counts as "arrived home" if the last EXIT was at least this
// long ago: boundary-noise oscillations (GPS error wider than the 150m fence)
// produce exit->enter pairs seconds apart, while a real trip out takes much
// longer. 45 minutes errs toward suppressing short errands — those days
// simply go without a check-in. If no EXIT has ever been recorded (fresh
// install / cleared data), trust-first lets the ENTER through — see
// handleEnter; otherwise the first arrival after every install would be
// silently swallowed.
private const val MIN_AWAY_MILLIS = 45 * 60 * 1000L

/**
 * Fires when Play Services detects the device entering the home geofence —
 * the only check-in trigger. Delegates to CheckinTrigger, same as
 * CheckinAlarmReceiver, so both paths produce identical behavior.
 */
class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive() called — GeofenceReceiver was invoked by the system")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.w(TAG, "GeofencingEvent.fromIntent() returned null")
            return
        }
        if (geofencingEvent.hasError()) {
            Log.w(TAG, "Geofencing error code: ${geofencingEvent.errorCode}")
            return
        }
        Log.d(TAG, "Geofence transition type: ${geofencingEvent.geofenceTransition}")
        when (geofencingEvent.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                HomeExitTracker.markExited(context)
                Log.i(TAG, "EXIT recorded — departure from home logged")
            }
            Geofence.GEOFENCE_TRANSITION_ENTER -> handleEnter(context)
            else -> Unit
        }
    }

    /**
     * Two-layer gate against phantom ENTERs (see HomeExitTracker for the
     * incident that motivated this): quiet hours block anything that lands
     * in the middle of the night regardless of state, and minimum-away-time
     * blocks doze/GPS-noise exit-enter oscillations that happen seconds
     * apart. An empty tracker (no EXIT ever recorded) is trusted instead of
     * blocked — first-run arrivals must not be swallowed. Every skip is
     * logged so a suppressed real arrival is visible in logcat instead of
     * silent.
     */
    private fun handleEnter(context: Context) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour >= QUIET_HOURS_START && hour < QUIET_HOURS_END) {
            Log.i(TAG, "ENTER ignored — quiet hours (${hour}:00); phantom doze-window enter lands here")
            return
        }
        val awayMillis = HomeExitTracker.millisSinceLastExit(context)
        if (awayMillis == null) {
            // Trust-first: no departure on record yet (fresh install / cleared
            // data), so there is nothing to gate against — suppressing here
            // would swallow the user's real first arrival. Quiet hours above
            // still apply; the 45-minute gate arms after the first EXIT.
            Log.i(TAG, "ENTER accepted — no EXIT on record yet (first-run); gate arms after first departure")
            CheckinTrigger.fire(context)
            return
        }
        if (awayMillis < MIN_AWAY_MILLIS) {
            Log.i(TAG, "ENTER ignored — exit too recent (away=${awayMillis}ms); phantom/noise enter")
            return
        }
        CheckinTrigger.fire(context)
    }
}
