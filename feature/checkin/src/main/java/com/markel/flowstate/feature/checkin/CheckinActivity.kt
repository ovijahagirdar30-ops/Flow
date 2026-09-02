package com.markel.flowstate.feature.checkin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint

/**
 * Launched directly by CheckinAlarmReceiver (Stage 4) so it can interrupt
 * whatever the user is doing — the same overlay-permission approach proven
 * to work reliably in the standalone Phase 0 test project.
 */
@AndroidEntryPoint
class CheckinActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CheckinScreen(onDismiss = { finish() })
                }
            }
        }
    }
}