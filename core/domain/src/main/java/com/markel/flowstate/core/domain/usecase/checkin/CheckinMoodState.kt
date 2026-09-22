package com.markel.flowstate.core.domain.checkin

/**
 * Step 1 of the evening check-in. Each value is 0–10. Lives in core:domain
 * since the eventual AI scheduling logic (also core:domain, likely) will
 * need to read this alongside tasks/habits — not just feature:checkin.
 */
data class CheckinMoodState(
    val energy: Int = 5,
    val sleepiness: Int = 5,
    val stress: Int = 5,
    val headache: Int = 0,
    val motivation: Int = 5,
    val energyComment: String = "",
    val sleepinessComment: String = "",
    val stressComment: String = "",
    val headacheComment: String = "",
    val motivationComment: String = "",
)