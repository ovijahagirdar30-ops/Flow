package com.markel.flowstate

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@HiltAndroidApp
class FlowStateApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
