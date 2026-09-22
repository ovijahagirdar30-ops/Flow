package com.markel.flowstate.core.data

import com.markel.flowstate.core.data.local.CheckinDao
import com.markel.flowstate.core.data.local.CheckinEntity
import com.markel.flowstate.core.domain.CheckinRepository
import com.markel.flowstate.core.domain.checkin.Checkin
import com.markel.flowstate.core.domain.checkin.CheckinMoodState
import com.markel.flowstate.core.domain.checkin.UnexpectedPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.LocalDate
import javax.inject.Inject

/**
 * Real implementation of CheckinRepository, same pattern as TaskRepositoryImpl.
 *
 * Unexpected plans are stored as one delimited/URL-encoded string column
 * rather than a separate table or a kotlinx.serialization JSON blob —
 * deliberately avoiding a new module dependency here, since
 * java.net.URLEncoder/Decoder need zero setup. Easy to swap for a proper
 * JSON column or a normalized table later if the AI brain ends up needing
 * to query individual plans directly.
 *
 * Mood comments are stored in their own nullable columns (v25 schema);
 * NULL maps to "" on read so pre-v25 rows and blank comments are identical
 * to callers.
 */
class CheckinRepositoryImpl @Inject constructor(
    private val dao: CheckinDao
) : CheckinRepository {

    override suspend fun saveTodayCheckin(mood: CheckinMoodState, unexpectedPlans: List<UnexpectedPlan>) {
        val entity = CheckinEntity(
            date = LocalDate.now().toString(),
            energy = mood.energy,
            sleepiness = mood.sleepiness,
            stress = mood.stress,
            headache = mood.headache,
            motivation = mood.motivation,
            unexpectedPlansEncoded = encodePlans(unexpectedPlans),
            energyComment = mood.energyComment,
            sleepinessComment = mood.sleepinessComment,
            stressComment = mood.stressComment,
            headacheComment = mood.headacheComment,
            motivationComment = mood.motivationComment
        )
        dao.upsertCheckin(entity)
    }

    override suspend fun getCheckinByDate(date: String): Checkin? {
        return dao.getCheckinByDate(date)?.toDomain()
    }

    override fun getAllCheckins(): Flow<List<Checkin>> {
        return dao.getAllCheckins().map { list -> list.map { it.toDomain() } }
    }

    private fun CheckinEntity.toDomain(): Checkin = Checkin(
        date = date,
        mood = CheckinMoodState(
            energy = energy,
            sleepiness = sleepiness,
            stress = stress,
            headache = headache,
            motivation = motivation,
            energyComment = energyComment.orEmpty(),
            sleepinessComment = sleepinessComment.orEmpty(),
            stressComment = stressComment.orEmpty(),
            headacheComment = headacheComment.orEmpty(),
            motivationComment = motivationComment.orEmpty()
        ),
        unexpectedPlans = decodePlans(unexpectedPlansEncoded)
    )

    private fun encodePlans(plans: List<UnexpectedPlan>): String =
        plans.joinToString("|||") { plan ->
            listOf(plan.description, plan.startTime, plan.durationMinutes.toString())
                .joinToString("::") { URLEncoder.encode(it, "UTF-8") }
        }

    private fun decodePlans(encoded: String): List<UnexpectedPlan> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split("|||").map { planStr ->
            val parts = planStr.split("::").map { URLDecoder.decode(it, "UTF-8") }
            UnexpectedPlan(
                description = parts[0],
                startTime = parts[1],
                durationMinutes = parts[2].toIntOrNull() ?: 0
            )
        }
    }
}