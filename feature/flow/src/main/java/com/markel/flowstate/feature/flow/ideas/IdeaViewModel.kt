package com.markel.flowstate.feature.flow.ideas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.core.domain.Category
import com.markel.flowstate.core.domain.CategoryRepository
import com.markel.flowstate.core.domain.Idea
import com.markel.flowstate.core.domain.IdeaRepository
import com.markel.flowstate.feature.flow.components.COLOR_TRANSPARENT
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IdeaEditorState(
    val idea: Idea? = null,
    val title: String = "",
    val content: String = "",
    val color: Long = COLOR_TRANSPARENT, // default: no background color
    val categoryId: Int? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class IdeaEditorViewModel @Inject constructor(
    private val ideaRepository: IdeaRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val _editor = MutableStateFlow(IdeaEditorState())
    val editor: StateFlow<IdeaEditorState> = _editor.asStateFlow()

    /** User categories, exposed so the editor can populate the category selector. */
    val categories: StateFlow<List<Category>> = categoryRepository.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Whether category tabs are enabled — the selector is only shown when true. */
    val categoriesEnabled: StateFlow<Boolean> = userPreferencesRepository.categoriesEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

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
    fun openNew(categoryId: Int? = null) {
        _editor.value = IdeaEditorState(categoryId = categoryId)
    }

    /** Opens the overlay to EDIT an existing idea. */
    fun openExisting(idea: Idea) {
        _editor.value = IdeaEditorState(
            idea = idea,
            title = idea.title,
            content = idea.content,
            color = idea.color,
            categoryId = idea.categoryId
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

    /**
     * Moves the idea being edited to a different category.
     *
     * `null` means General (no category). The change is reflected in the
     * editor state immediately and persisted through the existing autosave
     * flow (debounced [persistIfNeeded]).
     */
    fun updateCategory(categoryId: Int?) = _editor.update { it.copy(categoryId = categoryId) }

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
                    color = state.color,
                    categoryId = state.categoryId
                )
            )
        } else if (state.title.isNotBlank() || state.content.isNotBlank()) {
            val insertedId = ideaRepository.upsertIdea(
                Idea(
                    title = state.title,
                    content = state.content,
                    color = state.color,
                    createdAt = System.currentTimeMillis(),
                    categoryId = state.categoryId
                )
            )
            // Save the reference with the ID so subsequent autosaves don't create duplicates (existing != null)
            _editor.update { it.copy(idea = Idea(
                id = insertedId.toInt(),
                title = state.title,
                content = state.content,
                color = state.color,
                createdAt = System.currentTimeMillis(),
                categoryId = state.categoryId
            )) }
        }
    }

}