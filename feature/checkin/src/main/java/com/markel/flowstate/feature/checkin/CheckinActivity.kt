package com.markel.flowstate.feature.checkin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.markel.flowstate.core.data.AppColor
import com.markel.flowstate.core.data.MainTab
import com.markel.flowstate.core.data.ThemeMode
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.core.designsystem.theme.FlowStateTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Hosts the evening check-in (CheckinScreen): the designed greeting →
 * five-question flow → recap, then the legacy plans/tasks/plan steps,
 * plus the 9PM night review.
 *
 * Renders inside [FlowStateTheme] with the user's theme settings — same
 * typography (FlowStateTypography), color scheme and motion as MainActivity,
 * so the check-in is never a styled island in light mode / custom AppColor.
 * The theme's SideEffect also flips the status/navigation bar icon tint.
 */
@AndroidEntryPoint
class CheckinActivity : ComponentActivity() {

    @Inject lateinit var userPreferences: UserPreferencesRepository

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Appear over the lock screen (mirrors the manifest flags) and force the
        // display on. HyperOS ignores the turnScreenOn flag when the activity is
        // started from the background — which is exactly how the check-in fires —
        // so additionally take a timed ACQUIRE_CAUSES_WAKEUP wake lock: the
        // OEM-proof way to light the screen.
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        wakeDisplay()
        Log.i(TAG, "launched — display wake requested")
        // Black background with light (white) status/nav bar icons
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        // Two faces of the same wake-the-screen activity: the arrival
        // check-in (mood → plan) and the 9PM night review (day recap).
        val nightReview = intent.getStringExtra(EXTRA_MODE) == MODE_NIGHT_REVIEW
        val startAtPlan = intent.getStringExtra(EXTRA_START_STEP) == START_STEP_PLAN
        setContent {
            // Same five settings MainActivity feeds the theme — defaults only
            // cover the frame before DataStore's first emission.
            val themeMode by userPreferences.themeMode.collectAsState(ThemeMode.SYSTEM)
            val dynamicColor by userPreferences.dynamicColor.collectAsState(false)
            val pureSurfaces by userPreferences.pureSurfaces.collectAsState(false)
            val systemFont by userPreferences.systemFont.collectAsState(false)
            val selectedAppColor by userPreferences.selectedAppColor.collectAsState(AppColor.GREEN)

            FlowStateTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor,
                pureSurfaces = pureSurfaces,
                systemFont = systemFont,
                selectedAppColor = selectedAppColor
            ) {
                // Default Surface color = colorScheme.background, so the screen
                // follows the theme instead of a hardcoded black.
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (nightReview) {
                        NightReviewScreen(onDone = { finish() })
                    } else {
                        // Arrival check-in — geofence trigger and the Plan tab's
                        // manual "Start check-in" both land here, opening on the
                        // design's greeting. "Edit plan" jumps straight to PLAN.
                        CheckinScreen(
                            onDismiss = { finish() },
                            onOpenPlan = { openPlanTab() },
                            startAtPlan = startAtPlan
                        )
                    }
                }
            }
        }
    }

    /**
     * Forces the screen on for the check-in. Timed (30s) so the lock can never
     * leak, and ON_AFTER_RELEASE hands the display back to the normal system
     * timeout once released — the screen never stays lit indefinitely because
     * of us.
     */
    @Suppress("DEPRECATION")
    private fun wakeDisplay() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
            "flowstate:checkin-wake"
        ).apply { acquire(30_000L) }
    }

    override fun onDestroy() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
    }

    /**
     * Agree hand-off: starts the main app directly on the Plan checklist tab
     * and closes the check-in on top of it. Targets MainActivity by class-name
     * string — this module can't reference app-module classes — and the extra
     * is read once in MainActivity.onCreate (fresh launches only).
     */
    private fun openPlanTab() {
        val intent = Intent().apply {
            setClassName(packageName, "com.markel.flowstate.MainActivity")
            putExtra(EXTRA_OPEN_TAB, MainTab.PLAN.name)
        }
        startActivity(intent)
        finish()
    }

    companion object {
        /**
         * Intent extra MainActivity reads to open directly on a bottom-nav tab.
         * Value: a [MainTab] name (e.g. "PLAN"). Set by the check-in after
         * Agree — the feature module can't reference the app's MainActivity.
         */
        const val EXTRA_OPEN_TAB = "com.markel.flowstate.extra.OPEN_TAB"

        /**
         * Intent extra selecting which face of this activity to show.
         * Absent/anything else = the arrival check-in.
         */
        const val EXTRA_MODE = "com.markel.flowstate.extra.MODE"

        /** Value for [EXTRA_MODE]: the 9PM night review page. */
        const val MODE_NIGHT_REVIEW = "night_review"

        /**
         * Intent extra selecting which STEP the arrival check-in starts on.
         * Absent/anything else = the mood step (the normal first step).
         * Set by the Plan tab's "Edit plan" FAB item.
         */
        const val EXTRA_START_STEP = "com.markel.flowstate.extra.START_STEP"

        /** Value for [EXTRA_START_STEP]: the final plan step. */
        const val START_STEP_PLAN = "plan"

        private const val TAG = "CheckinActivity"
    }
}