// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.hiltAndroid) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.plugin.serialization) apply false
}

// Material3 1.5.0-alpha15 (used project-wide for the FAB menu) "strictly"
// forces Compose Foundation to 1.11.0-alpha06 — an early, unstable
// pre-release snapshot from Feb 2026. Stable Foundation has since moved on
// to 1.11.4, which very likely already fixes whatever caused
// "RowColumnParentData?.weight is internal" in that specific alpha build.
// force() is the one thing strong enough to override a transitive
// "strictly" constraint like that, applied here at the root so every
// module resolves the same, newer, more stable version consistently.
allprojects {
    configurations.all {
        resolutionStrategy {
            force(
                "androidx.compose.foundation:foundation:1.11.4",
                "androidx.compose.foundation:foundation-android:1.11.4",
                "androidx.compose.foundation:foundation-layout:1.11.4",
                "androidx.compose.foundation:foundation-layout-android:1.11.4"
            )
        }
    }
}