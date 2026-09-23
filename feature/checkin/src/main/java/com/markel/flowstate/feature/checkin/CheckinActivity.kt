package com.markel.flowstate.feature.checkin

import android.graphics.Color
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint

/**
 * Hosts the real 3-step evening check-in (CheckinScreen), replacing the
 * Stage 1 placeholder text now that mood/plans/tasks all exist.
 * Uses a black background with light status/navigation bars.
 */
@AndroidEntryPoint
class CheckinActivity : ComponentActivity() {

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
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color.Black
                ) {
                    CheckinScreen(onDismiss = { finish() })
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

    private companion object {
        const val TAG = "CheckinActivity"
    }
}