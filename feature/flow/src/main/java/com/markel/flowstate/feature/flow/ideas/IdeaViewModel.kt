package com.markel.flowstate.feature.flow.ideas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.domain.Idea
import com.markel.flowstate.core.domain.IdeaRepository
import com.markel.flowstate.feature.flow.ideas.components.IDEA_COLOR_TRANSPARENT
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IdeaEditorState(
    val idea: Idea? = null, // null = overlay closed
    val title: String = "",
    val content: String = "",
    val color: Long = IDEA_COLOR_TRANSPARENT // default: no background color
)

@HiltViewModel
class IdeaEditorViewModel @Inject constructor(
    private val ideaRepository: IdeaRepository
) : ViewModel() {
    private val _editor = MutableStateFlow(IdeaEditorState())
    val editor: StateFlow<IdeaEditorState> = _editor.asStateFlow()

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
     * Closes the overlay and auto-saves if there is something worth saving:
     * - Creating: saves if title or content is not blank.
     * - Editing: always updates (even if cleared).
     */
    fun closeAndSave() {
        val state = _editor.value
        viewModelScope.launch {
            val existingIdea = state.idea
            if (existingIdea != null) {
                // Editing — update in place
                ideaRepository.upsertIdea(
                    existingIdea.copy(
                        title = state.title,
                        content = state.content,
                        color = state.color
                    )
                )
            } else if (state.title.isNotBlank() || state.content.isNotBlank()) {
                // Creating — only persist if non-empty
                ideaRepository.upsertIdea(
                    Idea(
                        title = state.title,
                        content = state.content,
                        color = state.color,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
            _editor.value = IdeaEditorState() // reset / close
        }
    }

    /** Discards changes without saving (e.g. user explicitly cancels). */
    fun closeWithoutSaving() {
        _editor.value = IdeaEditorState()
    }

    // ── Field updates ─────────────────────────────────────────────────────────

    fun updateTitle(value: String) = _editor.update { it.copy(title = value) }

    fun updateContent(value: String) = _editor.update { it.copy(content = value) }

    fun updateColor(color: Long) = _editor.update { it.copy(color = color) }

}
