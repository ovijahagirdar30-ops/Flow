package com.markel.flowstate.feature.flow.ideas

import app.cash.turbine.test
import com.markel.flowstate.core.domain.Idea
import com.markel.flowstate.core.domain.IdeaRepository
import com.markel.flowstate.core.testing.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class IdeaViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: IdeaRepository = mockk(relaxed = true)
    private lateinit var viewModel: IdeaEditorViewModel

    @Test
    fun openNew_resetsEditorState() = runTest {
        // GIVEN - A fresh ViewModel instance
        viewModel = IdeaEditorViewModel(repository)

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
        viewModel = IdeaEditorViewModel(repository)
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
        viewModel = IdeaEditorViewModel(repository)

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
        viewModel = IdeaEditorViewModel(repository)
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
        viewModel = IdeaEditorViewModel(repository)
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
}