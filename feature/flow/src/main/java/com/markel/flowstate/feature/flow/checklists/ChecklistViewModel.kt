package com.markel.flowstate.feature.flow.checklists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.core.domain.Category
import com.markel.flowstate.core.domain.CategoryRepository
import com.markel.flowstate.core.domain.CheckList
import com.markel.flowstate.core.domain.CheckListItem
import com.markel.flowstate.core.domain.CheckListRepository
import com.markel.flowstate.feature.flow.components.COLOR_TRANSPARENT
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CheckListEditorState(
    val checkList: CheckList? = null,
    val title: String = "",
    val color: Long = COLOR_TRANSPARENT,
    val items: List<CheckListItem> = emptyList(),
    val categoryId: Int? = Category.GENERAL_ID
)

@OptIn(FlowPreview::class)
@HiltViewModel
class CheckListViewModel @Inject constructor(
    private val checkListRepository: CheckListRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _editor = MutableStateFlow(CheckListEditorState())
    val editor: StateFlow<CheckListEditorState> = _editor.asStateFlow()



    /** Handle to the debounced autosave collector so it can be canceled in */
    private var autosaveJob: Job? = null

    /** User categories, exposed so the editor can populate the category selector. */
    val categories: StateFlow<List<Category>> = categoryRepository.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Whether category tabs are enabled — the selector is only shown when true. */
    val categoriesEnabled: StateFlow<Boolean> = userPreferencesRepository.categoriesEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val generalCategoryName: StateFlow<String?> = userPreferencesRepository.generalCategoryName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        autosaveJob = viewModelScope.launch {
            _editor
                .drop(1)
                .debounce(800)
                .collect { state -> persistIfNeeded(state) }
        }
    }


    // ── Open / Close ──────────────────────────────────────────────────────────

    fun openNew(categoryId: Int? = Category.GENERAL_ID) {
        _editor.value = CheckListEditorState(categoryId = categoryId)
    }

    fun loadForEditing(checkListId: Int) {
        viewModelScope.launch {
            val lists = checkListRepository.getLists().first()
            val found = lists.firstOrNull { it.id == checkListId } ?: return@launch
            _editor.value = CheckListEditorState(
                checkList = found,
                title = found.title,
                color = found.color,
                items = found.items,
                categoryId = found.categoryId
            )
        }
    }

    fun closeAndSave() {
        autosaveJob?.cancel()
        viewModelScope.launch {
            persistIfNeeded(_editor.value)
        }
    }

    fun deleteCheckList(checkListId: Int) {
        viewModelScope.launch {
            val found = checkListRepository.getLists().first()
                .firstOrNull { it.id == checkListId } ?: return@launch
            checkListRepository.deleteList(found)
        }
    }

    override fun onCleared() {
        super.onCleared()
        _editor.value = CheckListEditorState()
    }

    // ── Field updates ─────────────────────────────────────────────────────────

    fun updateTitle(value: String) = _editor.update { it.copy(title = value) }

    fun updateColor(color: Long) = _editor.update { it.copy(color = color) }

    /**
     * Moves the checklist being edited to a different category.
     *
     * Pass [Category.GENERAL_ID] to move the checklist to the default (General)
     * category. The change is reflected in the editor state immediately and
     * persisted through the existing autosave flow (debounced [persistIfNeeded]).
     */
    fun updateCategory(categoryId: Int?) = _editor.update { it.copy(categoryId = categoryId ?: Category.GENERAL_ID) }

    fun addItem(): String {
        val newItem = CheckListItem(
            id = UUID.randomUUID().toString(),
            text = "",
            isDone = false,
            position = _editor.value.items.size
        )
        _editor.update { it.copy(items = it.items + newItem) }
        return newItem.id
    }

    fun updateItemText(id: String, text: String) {
        _editor.update { state ->
            state.copy(items = state.items.map { if (it.id == id) it.copy(text = text) else it })
        }
    }

    fun toggleItem(id: String) {
        _editor.update { state ->
            state.copy(items = state.items.map { if (it.id == id) it.copy(isDone = !it.isDone) else it })
        }
    }

    fun removeItem(id: String) {
        _editor.update { state ->
            state.copy(items = state.items.filter { it.id != id })
        }
    }

    fun reorderPendingItems(fromIndex: Int, toIndex: Int) {
        _editor.update { state ->
            val pending = state.items.filter { !it.isDone }.toMutableList()
            val completed = state.items.filter { it.isDone }
            val moved = pending.removeAt(fromIndex)
            pending.add(toIndex, moved)
            // Rebuild full list: reordered pending first, then completed unchanged
            val merged = (pending + completed).mapIndexed { i, item -> item.copy(position = i) }
            state.copy(items = merged)
        }
    }


    // ── Internal ──────────────────────────────────────────────────────────────

    private suspend fun persistIfNeeded(state: CheckListEditorState) {
        val itemsToSave = state.items
            .filter { it.text.isNotBlank() }
            .mapIndexed { i, item -> item.copy(position = i) }

        val existing = state.checkList
        if (existing != null) {
            checkListRepository.upsertList(
                existing.copy(
                    title = state.title,
                    color = state.color,
                    items = itemsToSave,
                    categoryId = state.categoryId
                )
            )
        } else if (state.title.isNotBlank() || itemsToSave.isNotEmpty()) {
            // First save of a new checklist — upsert and capture the assigned ID
            val newList = CheckList(
                title = state.title,
                color = state.color,
                items = itemsToSave,
                categoryId = state.categoryId
            )
            val assignedId = checkListRepository.upsertList(newList)

            // Update in-memory state so future saves use the real id (no duplicates)
            _editor.update { current ->
                current.copy(
                    checkList = CheckList(
                        id = assignedId,
                        title = current.title,
                        color = current.color,
                        items = itemsToSave,
                        categoryId = state.categoryId
                    )
                )
            }
        }
    }

}