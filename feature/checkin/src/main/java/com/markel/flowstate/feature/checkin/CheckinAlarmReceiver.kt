package com.markel.flowstate.feature.checkin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.markel.flowstate.core.notifications.CheckinAlarmScheduler
import com.markel.flowstate.core.notifications.EXTRA_NIGHT_REVIEW
import com.markel.flowstate.core.notifications.EXTRA_WEEKEND_CHECKIN
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Terminal for both alarm paths:
 *  - the nightly9PM night-review alarm ([EXTRA_NIGHT_REVIEW]): re-arms
 *    tomorrow's alarm FIRST (so a crash below can't break the chain), then
 *    hands off to [CheckinTrigger.fireNightReview] — the day-recap page,
 *    deliberately not debounced (it's a different page from the arrival
 *    check-in, and the alarm itself fires once per day);
 *  - the weekend 8AM alarm ([EXTRA_WEEKEND_CHECKIN]): re-arms next
 *    weekend's alarm FIRST, then hands off to [CheckinTrigger.fire] —
 *    the regular debounced check-in (home-only weekends never trip the
 *    arrival geofence, so time stands in for location);
 *  - a debug scheduleTest() alarm → [CheckinTrigger.fire] (debounced,
 *    arrival pipeline).
 */
@AndroidEntryPoint
class CheckinAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var checkinAlarmScheduler: CheckinAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_NIGHT_REVIEW, false)) {
            checkinAlarmScheduler.scheduleNightReview()
            CheckinTrigger.fireNightReview(context)
        } else if (intent.getBooleanExtra(EXTRA_WEEKEND_CHECKIN, false)) {
            checkinAlarmScheduler.scheduleWeekendCheckin()
            CheckinTrigger.fire(context)
        } else {
            CheckinTrigger.fire(context)
        }
    }
}
