package com.markel.flowstate.core.domain.checkin

data class Checkin(
    val date: String, // ISO date string, matches CheckinDebounce's LocalDate.now().toString() format
    val mood: CheckinMoodState,
    val unexpectedPlans: List<UnexpectedPlan>
)