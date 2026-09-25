package com.markel.flowstate.core.notifications

import android.content.Context
import java.time.LocalDate

/**
 * Prevents the check-in from firing more than once per calendar day.
 * Geofence ENTER fires every time you cross into the radius (leave and
 * come back = another ENTER), and the debug test alarm could overlap a
 * real arrival — both paths go through this via CheckinTrigger before
 * anything is shown.
 */
object CheckinDebounce {
    private const val PREFS_NAME = "checkin_debounce"
    private const val KEY_LAST_CHECKIN_DATE = "last_checkin_date"

    fun hasCheckedInToday(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastDate = prefs.getString(KEY_LAST_CHECKIN_DATE, null) ?: return false
        return lastDate == LocalDate.now().toString()
    }

    fun markCheckedInToday(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_CHECKIN_DATE, LocalDate.now().toString()).apply()
    }

    /** Manual-testing only — clears today's flag so scheduleTest() can refire without waiting for midnight. */
    fun resetForTesting(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_LAST_CHECKIN_DATE).apply()
    }
}
