package com.markel.flowstate.feature.flow.ideas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.domain.Idea
import com.markel.flowstate.core.domain.IdeaRepository
import com.markel.flowstate.feature.flow.components.COLOR_TRANSPARENT
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IdeaEditorState(
    val idea: Idea? = null,
    val title: String = "",
    val content: String = "",
    val color: Long = COLOR_TRANSPARENT // default: no background color
)

@OptIn(FlowPreview::class)
@HiltViewModel
class IdeaEditorViewModel @Inject constructor(
    private val ideaRepository: IdeaRepository
) : ViewModel() {
    private val _editor = MutableStateFlow(IdeaEditorState())
    val editor: StateFlow<IdeaEditorState> = _editor.asStateFlow()

    init {
        // Autosave: debounce of 800ms
        // drop(1) to drop the initial state when opening the editor
        viewModelScope.launch {
            _editor
                .drop(1)
                .debounce(800)
                .collect { state -> persistIfNeeded(state) }
        }
    }

    // ── Open / Close ──────────────────────────────────────────────────────────

    /** Opens the overlay to CREATE a new blank idea. */
    fun openNew() {
        _editor.value = IdeaEditorState()
    }

    /** Opens the overlay to EDIT an existing idea. */
    fun openExisting(idea: Idea) {
        _editor.value = IdeaEditorState(
            idea = idea,
            title = idea.title,
            content = idea.content,
            color = idea.color
        )
    }

    /**
     * Loads an idea by ID from the repository.
     * Used when navigating (IdeaEditorScreen) with only the ID.
     */
    fun loadIdeaForEditing(ideaId: Int) {
        viewModelScope.launch {
            val idea = ideaRepository.getIdeaById(ideaId)
            if (idea != null) openExisting(idea)
        }
    }

    fun closeAndSave() {
        viewModelScope.launch {
            persistIfNeeded(_editor.value)
            _editor.value = IdeaEditorState()  // reset
        }
    }

    /** Discards changes without saving (e.g. user explicitly cancels (not implemented yet)). */
    fun closeWithoutSaving() {
        _editor.value = IdeaEditorState()
    }

    // ── Field updates ─────────────────────────────────────────────────────────

    fun updateTitle(value: String) = _editor.update { it.copy(title = value) }

    fun updateContent(value: String) = _editor.update { it.copy(content = value) }

    fun updateColor(color: Long) = _editor.update { it.copy(color = color) }

    fun deleteIdea(ideaId: Int) {
        viewModelScope.launch {
            val idea = ideaRepository.getIdeaById(ideaId) ?: return@launch
            ideaRepository.deleteIdea(idea)
            _editor.value = IdeaEditorState()
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private suspend fun persistIfNeeded(state: IdeaEditorState) {
        val existing = state.idea
        if (existing != null) {
            ideaRepository.upsertIdea(
                existing.copy(
                    title = state.title,
                    content = state.content,
                    color = state.color
                )
            )
        } else if (state.title.isNotBlank() || state.content.isNotBlank()) {
            ideaRepository.upsertIdea(
                Idea(
                    title = state.title,
                    content = state.content,
                    color = state.color,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }


}