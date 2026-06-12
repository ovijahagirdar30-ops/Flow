package com.markel.flowstate.core.data.backup

import com.markel.flowstate.core.data.local.CheckListDao
import com.markel.flowstate.core.data.local.HabitDao
import com.markel.flowstate.core.data.local.IdeaDao
import com.markel.flowstate.core.data.local.TaskDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val ideaDao: IdeaDao,
    private val checkListDao: CheckListDao,
    private val habitDao: HabitDao
) : BackupRepository {

    /** Lenient parser — ignores unknown keys so older backups still load. */
    private val lenientJson = Json { ignoreUnknownKeys = true }

    /** Pretty-printed serializer for export files. */
    private val exportJson = Json { prettyPrint = true }

    // ── Export ────────────────────────────────────────────────────────

    override suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val tasksWithSubTasks = taskDao.getAllTasksOnce()
        val allListsWithItems = checkListDao.getAllListsOnce()

        val export = FlowStateExport(
            tasks = tasksWithSubTasks.map { it.task.toSchema() },
            subTasks = tasksWithSubTasks.flatMap { it.subTasks.map { s -> s.toSchema() } },
            ideas = ideaDao.getAllIdeasOnce().map { it.toSchema() },
            checkLists = allListsWithItems.map { it.list.toSchema() },
            checkListItems = allListsWithItems.flatMap { it.items.map { i -> i.toSchema() } },
            habits = habitDao.getAllHabitsOnce().map { it.habit.toSchema() },
            habitEntries = habitDao.getAllEntriesOnce().map { it.toSchema() },
            habitNumericEntries = habitDao.getAllNumericEntriesOnce().map { it.toSchema() }
        )

        exportJson.encodeToString(FlowStateExport.serializer(), export)
    }

    // ── Restore ───────────────────────────────────────────────────────

    override suspend fun restoreFromJson(json: String): RestoreResult =
        withContext(Dispatchers.IO) {
            try {
                val data = lenientJson.decodeFromString<FlowStateExport>(json)

                if (data.schemaVersion != FlowStateExport.CURRENT_SCHEMA_VERSION) {
                    return@withContext RestoreResult.Error(RestoreErrorType.SCHEMA_MISMATCH)
                }

                // Additive restore — upsert everything
                data.tasks.map { it.toEntity() }.forEach { taskDao.upsertTaskEntity(it) }
                if (data.subTasks.isNotEmpty()) taskDao.insertSubTasks(data.subTasks.map { it.toEntity() })
                data.ideas.map { it.toEntity() }.forEach { ideaDao.upsertIdea(it) }
                data.checkLists.map { it.toEntity() }.forEach { checkListDao.upsertListEntity(it) }
                if (data.checkListItems.isNotEmpty()) checkListDao.insertListItems(data.checkListItems.map { it.toEntity() })
                data.habits.map { it.toEntity() }.forEach { habitDao.insertHabit(it) }
                data.habitEntries.map { it.toEntity() }.forEach { habitDao.insertEntry(it) }
                data.habitNumericEntries.map { it.toEntity() }.forEach { habitDao.upsertNumericEntry(it) }

                RestoreResult.Success
            } catch (e: IllegalArgumentException) {
                RestoreResult.Error(RestoreErrorType.INVALID_FILE)
            } catch (e: Exception) {
                RestoreResult.Error(RestoreErrorType.UNKNOWN)
            }
        }
}
