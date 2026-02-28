package com.markel.flowstate.feature.flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.core.domain.CheckListRepository
import com.markel.flowstate.core.domain.GridItemType
import com.markel.flowstate.core.domain.GridOrderEntry
import com.markel.flowstate.core.domain.GridOrderRepository
import com.markel.flowstate.core.domain.Idea
import com.markel.flowstate.core.domain.IdeaRepository
import com.markel.flowstate.core.domain.Priority
import com.markel.flowstate.core.domain.SubTask
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.core.domain.TaskRepository
import com.markel.flowstate.core.domain.usecase.DeleteTaskUseCase
import com.markel.flowstate.core.domain.usecase.ToggleTaskUseCase
import com.markel.flowstate.feature.flow.tasks.TaskViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * FlowScreen coordinator.
 *
 * Responsibilities:
 *  - Control cross-screen UI state (isGridView)
 *  - Combine Tasks, Ideas, and CheckLists flows for the Grid view
 *  - Delegate task CRUD operations to TaskViewModel
 *  - Directly manage Ideas and CheckLists
 */
@HiltViewModel
class FlowViewModel @Inject constructor(
    // Repositories required for the unified grid
    private val taskRepository: TaskRepository,
    private val ideaRepository: IdeaRepository,
    private val checkListRepository: CheckListRepository,
    private val gridOrderRepository: GridOrderRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _localItems = MutableStateFlow<List<GridItem>?>(null)

    // ── View state ───────────────────────────────────────────────────────

    val isGridView: StateFlow<Boolean?> = userPreferencesRepository.isGridView
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun toggleView() {
        viewModelScope.launch {
            userPreferencesRepository.setGridView(!(isGridView.value ?: false))
        }
    }

    // ── Unified Grid state ─────────────────────────────────────────────

    val flowUiState: StateFlow<FlowUiState> = combine(
        taskRepository.getTasks(),
        ideaRepository.getIdeas(),
        checkListRepository.getLists(),
        gridOrderRepository.getOrder(),
        _localItems
    ) { tasks, ideas, checkLists, order, localItems ->
        if (localItems != null) return@combine FlowUiState.Success(localItems)

        val taskMap = tasks.filter { !it.isDone }.associateBy { it.id }
        val ideaMap = ideas.associateBy { it.id }
        val checkListMap = checkLists.associateBy { it.id }

        // Items that already have a saved position, respecting the order
        val orderedItems = order.mapNotNull { entry ->
            when (entry.itemType) {
                GridItemType.TASK -> taskMap[entry.itemId]?.let { GridItem.TaskItem(it) }
                GridItemType.IDEA -> ideaMap[entry.itemId]?.let { GridItem.IdeaItem(it) }
                GridItemType.CHECKLIST -> checkListMap[entry.itemId]?.let {
                    GridItem.CheckListItem(
                        it
                    )
                }
            }
        }

        // New items that do not yet have an entry in grid_order (added to the beginning)
        val orderedKeys = order.map { it.itemId to it.itemType }.toSet()
        val newItems = buildList {
            taskMap.values
                .filter { (it.id to GridItemType.TASK) !in orderedKeys }
                .forEach { add(GridItem.TaskItem(it)) }
            ideaMap.values
                .filter { (it.id to GridItemType.IDEA) !in orderedKeys }
                .forEach { add(GridItem.IdeaItem(it)) }
            checkListMap.values
                .filter { (it.id to GridItemType.CHECKLIST) !in orderedKeys }
                .forEach { add(GridItem.CheckListItem(it)) }
        }
        FlowUiState.Success(newItems + orderedItems)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FlowUiState.Loading
    )

    // ── Reorder ────────────────────────────────────────────────────────

    fun onGridReorder(fromIndex: Int, toIndex: Int) {
        val currentItems = (_localItems.value
            ?: (flowUiState.value as? FlowUiState.Success)?.items)
            ?: return

        val mutable = currentItems.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)

        _localItems.value = mutable
    }

    fun onGridDragEnd() {
        val finalItems = _localItems.value ?: return

        val entries = finalItems.mapIndexed { index, gridItem ->
            when (gridItem) {
                is GridItem.TaskItem      -> GridOrderEntry(gridItem.task.id, GridItemType.TASK, index)
                is GridItem.IdeaItem      -> GridOrderEntry(gridItem.idea.id, GridItemType.IDEA, index)
                is GridItem.CheckListItem -> GridOrderEntry(gridItem.checkList.id, GridItemType.CHECKLIST, index)
            }
        }

        viewModelScope.launch {
            gridOrderRepository.saveOrder(entries)
            _localItems.value = null
        }
    }

    // ── Register new/deleted items ────────────────────────────

    fun registerItem(itemId: Int, type: GridItemType) {
        viewModelScope.launch { gridOrderRepository.addEntry(itemId, type) }
    }

    fun unregisterItem(itemId: Int, type: GridItemType) {
        viewModelScope.launch { gridOrderRepository.removeEntry(itemId, type) }
    }

}