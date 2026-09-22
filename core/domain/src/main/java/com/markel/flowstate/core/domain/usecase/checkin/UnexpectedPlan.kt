package com.markel.flowstate.core.domain.checkin

data class UnexpectedPlan(
    val description: String,
    val startTime: String, // plain string for now; a proper time type can replace this later
    val durationMinutes: Int
)