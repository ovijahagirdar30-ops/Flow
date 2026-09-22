package com.markel.flowstate.feature.checkin

/**
 * Home coordinates for the arrival geofence — a thin indirection so the REAL
 * values live in HomeLocationLocal.kt, which is gitignored and never enters
 * version control. That keeps your actual address permanently out of the
 * repository's history, instead of relying on remembering not to stage this
 * file (which failed once already).
 *
 * HomeLocationLocal.kt must live in this same package and look like:
 *
 *   object HomeLocationLocal {
 *       const val LATITUDE: Double = <your latitude>
 *       const val LONGITUDE: Double = <your longitude>
 *       const val RADIUS_METERS: Float = 150f
 *   }
 *
 * It is intentionally not committed — create it locally if missing (the app
 * won't compile without it). A future "set home location" settings screen
 * should replace both objects.
 */
object HomeLocation {
    val LATITUDE: Double = HomeLocationLocal.LATITUDE
    val LONGITUDE: Double = HomeLocationLocal.LONGITUDE
    val RADIUS_METERS: Float = HomeLocationLocal.RADIUS_METERS
}
