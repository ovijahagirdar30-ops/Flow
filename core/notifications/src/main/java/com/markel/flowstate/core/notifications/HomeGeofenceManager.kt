package com.markel.flowstate.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val GEOFENCE_RECEIVER_CLASS = "com.markel.flowstate.feature.checkin.GeofenceReceiver"
private const val HOME_GEOFENCE_ID = "home_arrival"
private const val GEOFENCE_REQUEST_CODE = 9003
private const val TAG = "HomeGeofenceManager"

/**
 * Registers the "arrived home" geofence with Play Services. Targets
 * GeofenceReceiver (feature:checkin) by class-name string, same pattern as
 * CheckinAlarmScheduler targeting CheckinAlarmReceiver.
 */
@Singleton
class HomeGeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun geofencePendingIntent(): PendingIntent {
        val intent = Intent().apply {
            component = ComponentName(context.packageName, GEOFENCE_RECEIVER_CLASS)
        }
        // FLAG_MUTABLE, not IMMUTABLE — Play Services writes the
        // GeofencingEvent extras (transition type, triggering geofence) onto
        // this intent when it delivers the broadcast, and rejects a
        // registration with ApiException 10 "PendingIntent must be mutable"
        // if it's immutable. This is a hard requirement of the Geofencing API.
        return PendingIntent.getBroadcast(
            context,
            GEOFENCE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Registers the home-arrival geofence. Safe to call repeatedly — reuses
     * the same geofence ID, so Play Services replaces any existing
     * registration rather than stacking duplicates. Logs success/failure —
     * check Logcat (tag "HomeGeofenceManager") to confirm registration
     * actually succeeded, since this call is otherwise silent.
     */
    @Suppress("MissingPermission") // checked via hasLocationPermission() by the caller
    fun registerHomeGeofence(latitude: Double, longitude: Double, radiusMeters: Float) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "registerHomeGeofence() skipped — ACCESS_FINE_LOCATION not granted")
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId(HOME_GEOFENCE_ID)
            .setCircularRegion(latitude, longitude, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        // setInitialTrigger(0) = no initial trigger, deliberately: an ENTER
        // initial trigger would fire the check-in the moment the app is opened
        // while already home (registration happens on every app launch), which
        // immediately marks CheckinDebounce's once-per-day flag — silently
        // killing the REAL arrival check-in later that day. The geofence should
        // only fire on an actual outside->inside transition.
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(0)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(request, geofencePendingIntent())
            .addOnSuccessListener {
                Log.d(TAG, "Geofence registered successfully at ($latitude, $longitude), radius=${radiusMeters}m")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Geofence registration FAILED: ${e.message}", e)
            }
    }

    fun removeHomeGeofence() {
        geofencingClient.removeGeofences(geofencePendingIntent())
    }
}