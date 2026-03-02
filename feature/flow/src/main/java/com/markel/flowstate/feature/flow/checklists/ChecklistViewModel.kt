package com.markel.flowstate.feature.flow.checklists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.domain.CheckList
import com.markel.flowstate.core.domain.CheckListItem
import com.markel.flowstate.core.domain.CheckListRepository
import com.markel.flowstate.feature.flow.components.COLOR_TRANSPARENT
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CheckListEditorState(
    val checkList: CheckList? = null,
    val title: String = "",
    val color: Long = COLOR_TRANSPARENT,
    val items: List<CheckListItem> = emptyList()
)

@HiltViewModel
class CheckListViewModel @Inject constructor(
    private val checkListRepository: CheckListRepository
) : ViewModel() {

    private val _editor = MutableStateFlow(CheckListEditorState())
    val editor: StateFlow<CheckListEditorState> = _editor.asStateFlow()

    // ── Open / Close ──────────────────────────────────────────────────────────

    fun openNew() {
        _editor.value = CheckListEditorState()
    }

    fun loadForEditing(checkListId: Int) {
        viewModelScope.launch {
            val lists = checkListRepository.getLists().first()
            val found = lists.firstOrNull { it.id == checkListId } ?: return@launch
            _editor.value = CheckListEditorState(
                checkList = found,
                title = found.title,
                color = found.color,
                items = found.items
            )
        }
    }

    fun closeAndSave() {
        val state = _editor.value
        val itemsToSave = state.items.filter { it.text.isNotBlank() }.mapIndexed { i, item -> item.copy(position = i) }
        viewModelScope.launch {
            val existing = state.checkList
            if (existing != null) {
                checkListRepository.upsertList(
                    existing.copy(
                        title = state.title,
                        color = state.color,
                        items = itemsToSave
                    )
                )
            } else if (state.title.isNotBlank() || itemsToSave.isNotEmpty()) {
                checkListRepository.upsertList(
                    CheckList(
                        title = state.title,
                        color = state.color,
                        items = itemsToSave
                    )
                )
            }
            _editor.value = CheckListEditorState()
        }
    }

    fun deleteCheckList(checkListId: Int) {
        viewModelScope.launch {
            val found = checkListRepository.getLists().first()
                .firstOrNull { it.id == checkListId } ?: return@launch
            checkListRepository.deleteList(found)
            _editor.value = CheckListEditorState()
        }
    }

    // ── Field updates ─────────────────────────────────────────────────────────

    fun updateTitle(value: String) = _editor.update { it.copy(title = value) }

    fun updateColor(color: Long) = _editor.update { it.copy(color = color) }

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

    fun reorderItems(fromIndex: Int, toIndex: Int) {
        _editor.update { state ->
            val mutable = state.items.toMutableList()
            val item = mutable.removeAt(fromIndex)
            mutable.add(toIndex, item)
            state.copy(items = mutable.mapIndexed { i, it -> it.copy(position = i) })
        }
    }
}