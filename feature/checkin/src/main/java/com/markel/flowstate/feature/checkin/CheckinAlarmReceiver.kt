package com.markel.flowstate.feature.checkin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Fires on a debug scheduleTest() alarm (the 9PM fallback was removed —
 * arriving home via GeofenceReceiver is the only real trigger). Delegates
 * to CheckinTrigger so test firings behave identically to geofence
 * firings, including the once-per-day debounce.
 */
class CheckinAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        CheckinTrigger.fire(context)
    }
}
