package com.markel.flowstate.feature.checkin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

private const val TAG = "GeofenceReceiver"

/**
 * Fires when Play Services detects the device entering the home geofence —
 * the primary check-in trigger. Delegates to CheckinTrigger, same as
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
        if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            CheckinTrigger.fire(context)
        }
    }
}