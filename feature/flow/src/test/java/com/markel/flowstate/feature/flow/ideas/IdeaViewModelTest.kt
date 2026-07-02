package com.markel.flowstate.feature.flow.ideas

import app.cash.turbine.test
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.core.domain.CategoryRepository
import com.markel.flowstate.core.domain.Idea
import com.markel.flowstate.core.domain.IdeaRepository
import com.markel.flowstate.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class IdeaViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: IdeaRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)
    private lateinit var viewModel: IdeaEditorViewModel

    @Test
    fun openNew_resetsEditorState() = runTest {
        // GIVEN - A fresh ViewModel instance
        viewModel = IdeaEditorViewModel(repository, categoryRepository, userPreferencesRepository)

        // WHEN - Opening the editor for a new idea
        viewModel.openNew()

        // THEN - The editor state should be clean (null idea and empty fields)
        viewModel.editor.test {
            val state = awaitItem()
            assertNull(state.idea)
            assertEquals("", state.title)
            assertEquals("", state.content)
        }
    }

    @Test
    fun openExisting_loadsIdeaData() = runTest {
        // GIVEN - An existing idea and the ViewModel
        viewModel = IdeaEditorViewModel(repository, categoryRepository, userPreferencesRepository)
        val idea = Idea(id = 1, title = "Original", content = "Content", color = 0xFF123456L)

        // WHEN - Opening the editor with that existing idea
        viewModel.openExisting(idea)

        // THEN - The editor state must reflect the idea's current data
        viewModel.editor.test {
            val state = awaitItem()
            assertEquals(idea, state.idea)
            assertEquals("Original", state.title)
            assertEquals(0xFF123456L, state.color)
        }
    }

    @Test
    fun updateFields_updatesStateCorrectly() = runTest {
        // GIVEN - A ViewModel in its initial state
        viewModel = IdeaEditorViewModel(repository, categoryRepository, userPreferencesRepository)

        // WHEN - The user types a new title, content and changes the color
        viewModel.updateTitle("New Idea")
        viewModel.updateContent("Content of the idea")
        viewModel.updateColor(0xFF654321L)

        // THEN - The UI state should be updated with the new values immediately
        viewModel.editor.test {
            val state = awaitItem()
            assertEquals("New Idea", state.title)
            assertEquals("Content of the idea", state.content)
            assertEquals(0xFF654321L, state.color)
        }
    }

    @Test
    fun closeAndSave_withNewIdea_callsRepository() = runTest {
        // GIVEN - A new idea being composed in the editor
        viewModel = IdeaEditorViewModel(repository, categoryRepository, userPreferencesRepository)
        viewModel.updateTitle("New Title")
        viewModel.updateContent("New Content")

        // WHEN - Closing the editor (which triggers the save logic)
        viewModel.closeAndSave()

        // THEN - The repository's upsert method should be called with the new data
        coVerify {
            repository.upsertIdea(match {
                it.title == "New Title" && it.content == "New Content"
            })
        }
    }

    @Test
    fun closeAndSave_withExistingIdea_updatesIdea() = runTest {
        // GIVEN - An editor already loaded with an existing idea
        viewModel = IdeaEditorViewModel(repository, categoryRepository, userPreferencesRepository)
        val idea = Idea(id = 10, title = "Old Title", content = "Old Content", color = 0L)
        viewModel.openExisting(idea)

        // WHEN - Modifying the title and saving
        viewModel.updateTitle("Updated Title")
        viewModel.closeAndSave()

        // THEN - The repository should receive an update for the specific ID with the new title
        coVerify {
            repository.upsertIdea(match {
                it.id == 10 && it.title == "Updated Title"
            })
        }
    }

    /**
     * Test case for a known bug: autosave was triggering multiple times while
     * the user was typing a new idea. upsertIdea should only be called once for
     * the initial insertion; subsequent calls should be updates to the same idea (same ID).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun autosave_newIdea_doesNotCreateDuplicates() = runTest {
        // GIVEN - The repository returns ID=42 on the first insertion
        coEvery { repository.upsertIdea(any()) } returns 42L
        viewModel = IdeaEditorViewModel(repository, categoryRepository, userPreferencesRepository)
        viewModel.openNew()

        // WHEN - The user types in two separate bursts (2 debounce triggers)
        viewModel.updateTitle("My idea")
        advanceTimeBy(900) // first autosave → should insert and save ID=42

        viewModel.updateContent("More text")
        advanceTimeBy(900) // second autosave → should update id=42, NOT insert a new one

        // THEN - upsertIdea was called exactly twice...
        coVerify(exactly = 2) { repository.upsertIdea(any()) }
        // ...but the second call used ID 42 (update), not id=0 (new insertion)
        coVerify(exactly = 1) {
            repository.upsertIdea(match { it.id == 42 && it.content == "More text" })
        }
    }

    /**
     * After the first autosave of a new idea, the internal state must have
     * `idea != null` with the actual ID returned by the repository.
     * If this fails, the next autosave would recreate the idea from scratch.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun autosave_newIdea_updatesStateWithInsertedId() = runTest {
        // GIVEN
        coEvery { repository.upsertIdea(any()) } returns 99L
        viewModel = IdeaEditorViewModel(repository, categoryRepository, userPreferencesRepository)
        viewModel.openNew()

        // WHEN - The debounce triggers
        viewModel.updateTitle("Title")
        advanceTimeBy(900)

        // THEN - The state already contains the idea with the real ID
        viewModel.editor.test {
            val state = awaitItem()
            assertNotNull(state.idea)
            assertEquals(99, state.idea?.id)
        }
    }

    /**
     * If the editor is closed before the debounce triggers (rapid typing + back),
     * closeAndSave must still persist the idea exactly once.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun closeAndSave_beforeDebounce_savesExactlyOnce() = runTest {
        coEvery { repository.upsertIdea(any()) } returns 1L
        viewModel = IdeaEditorViewModel(repository, categoryRepository, userPreferencesRepository)
        viewModel.openNew()

        // WHEN - User types and closes before 800ms
        viewModel.updateTitle("Fast")
        viewModel.closeAndSave() // manually save before debounce

        advanceTimeBy(900) // debounce should no longer do anything (state reset)

        // THEN - Only one insertion
        coVerify(exactly = 1) {
            repository.upsertIdea(match { it.title == "Fast" })
        }
    }

    /**
     * A completely blank idea should never be persisted,
     * neither by autosave nor by closeAndSave.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun autosave_blankIdea_neverCallsRepository() = runTest {
        viewModel = IdeaEditorViewModel(repository, categoryRepository, userPreferencesRepository)
        viewModel.openNew()

        advanceTimeBy(900)
        viewModel.closeAndSave()

        coVerify(exactly = 0) { repository.upsertIdea(any()) }
    }

    /**
     * Editing an existing idea should never create a new one,
     * regardless of how many times the autosave triggers.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun autosave_existingIdea_neverCreatesNewIdea() = runTest {
        coEvery { repository.upsertIdea(any()) } returns 5L
        viewModel = IdeaEditorViewModel(repository, categoryRepository, userPreferencesRepository)
        val idea = Idea(id = 5, title = "Original", content = "", color = 0L)
        viewModel.openExisting(idea)

        // WHEN - Multiple writing bursts
        viewModel.updateTitle("Edit 1"); advanceTimeBy(900)
        viewModel.updateTitle("Edit 2"); advanceTimeBy(900)
        viewModel.updateTitle("Edit 3"); advanceTimeBy(900)

        // THEN - All calls use id=5, never id=0
        coVerify(atLeast = 1) {
            repository.upsertIdea(match { it.id == 5 })
        }
        coVerify(exactly = 0) {
            repository.upsertIdea(match { it.id == 0 })
        }
    }

    /**
     * deleteIdea must call the repository with the correct idea
     * and reset the editor state.
     */
    @Test
    fun deleteIdea_callsRepositoryAndResetsState() = runTest {
        val idea = Idea(id = 7, title = "To delete", content = "", color = 0L)
        coEvery { repository.getIdeaById(7) } returns idea
        viewModel = IdeaEditorViewModel(repository, categoryRepository, userPreferencesRepository)
        viewModel.openExisting(idea)

        viewModel.deleteIdea(7)

        coVerify { repository.deleteIdea(idea) }
        viewModel.editor.test {
            val state = awaitItem()
            assertNull(state.idea)
            assertEquals("", state.title)
        }
    }
}