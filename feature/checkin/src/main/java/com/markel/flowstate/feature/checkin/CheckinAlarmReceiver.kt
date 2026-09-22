package com.markel.flowstate.feature.checkin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.markel.flowstate.core.notifications.CheckinAlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Fires on the fallback cutoff alarm (or manual scheduleTest()). Delegates
 * the actual check-in popup to CheckinTrigger, same as GeofenceReceiver,
 * then unconditionally re-schedules tomorrow's cutoff — this has to happen
 * every time regardless of whether CheckinTrigger actually showed anything
 * (e.g. geofence already checked you in today), or the cutoff alarm would
 * only ever fire once and never again.
 */
@AndroidEntryPoint
class CheckinAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var checkinAlarmScheduler: CheckinAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        CheckinTrigger.fire(context)
        checkinAlarmScheduler.scheduleFallbackCutoff()
    }
}