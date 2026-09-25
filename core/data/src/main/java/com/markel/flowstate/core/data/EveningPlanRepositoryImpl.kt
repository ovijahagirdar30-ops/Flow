package com.markel.flowstate.core.data

import com.markel.flowstate.core.data.local.EveningPlanDao
import com.markel.flowstate.core.data.local.EveningPlanEntity
import com.markel.flowstate.core.domain.EveningPlan
import com.markel.flowstate.core.domain.EveningPlanRepository
import com.markel.flowstate.core.domain.PlanBlock
import com.markel.flowstate.core.domain.PlanBlockKind
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Room-backed [EveningPlanRepository]. Blocks cross the storage boundary as
 * JSON via private DTOs — deliberately keeping EveningPlan itself free of
 * serialization annotations (its doc: provider response quirks map at the
 * boundary, never inward). `kind` is stored as the enum's name string so
 * DTOs never need an enum serializer, with valueOf failing loudly rather
 * than silently corrupting a plan if the enum is ever renamed.
 */
class EveningPlanRepositoryImpl @Inject constructor(
    private val dao: EveningPlanDao
) : EveningPlanRepository {

    private val json = Json

    override suspend fun saveAgreedPlan(plan: EveningPlan) {
        dao.upsertPlan(
            EveningPlanEntity(
                date = plan.date,
                headline = plan.headline,
                generatedAtMillis = plan.generatedAtMillis,
                blocksJson = json.encodeToString(plan.blocks.map { it.toStored() }),
                checkedIndexesJson = null // nothing ticked the moment it's agreed
            )
        )
    }

    override suspend fun latestAgreedPlan(): EveningPlan? =
        dao.latestPlan()?.toDomain()

    override suspend fun checkedIndexes(date: String): List<Int> =
        dao.checkedIndexesJson(date)
            ?.let { json.decodeFromString<List<Int>>(it) }
            .orEmpty()

    override suspend fun setCheckedIndexes(date: String, indexes: List<Int>) {
        dao.updateCheckedIndexesJson(
            date = date,
            json = indexes.distinct().sorted()
                .takeIf { it.isNotEmpty() }
                ?.let { json.encodeToString(it) }
        )
    }

    private fun EveningPlanEntity.toDomain(): EveningPlan = EveningPlan(
        date = date,
        generatedAtMillis = generatedAtMillis,
        headline = headline,
        blocks = json.decodeFromString<List<StoredBlock>>(blocksJson).map { it.toDomain() }
    )

    private fun PlanBlock.toStored() = StoredBlock(
        startTime = startTime,
        durationMinutes = durationMinutes,
        title = title,
        reason = reason,
        kind = kind.name,
        referenceId = referenceId
    )

    private fun StoredBlock.toDomain() = PlanBlock(
        startTime = startTime,
        durationMinutes = durationMinutes,
        title = title,
        reason = reason,
        kind = PlanBlockKind.valueOf(kind),
        referenceId = referenceId
    )

    @Serializable
    private data class StoredBlock(
        val startTime: String,
        val durationMinutes: Int,
        val title: String,
        val reason: String,
        val kind: String,
        val referenceId: Int? = null
    )
}
