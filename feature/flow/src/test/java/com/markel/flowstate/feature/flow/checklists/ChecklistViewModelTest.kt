package com.markel.flowstate.feature.flow.checklists

import app.cash.turbine.test
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.core.domain.CategoryRepository
import com.markel.flowstate.core.domain.CheckList
import com.markel.flowstate.core.domain.CheckListItem
import com.markel.flowstate.core.domain.CheckListRepository
import com.markel.flowstate.core.testing.util.MainDispatcherRule
import com.markel.flowstate.feature.flow.components.COLOR_TRANSPARENT
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChecklistViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: CheckListRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)
    private lateinit var viewModel: CheckListViewModel

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun item(id: String, text: String, isDone: Boolean = false, position: Int = 0) =
        CheckListItem(id = id, text = text, isDone = isDone, position = position)

    private fun checklist(
        id: Int = 1,
        title: String = "My List",
        items: List<CheckListItem> = emptyList()
    ) = CheckList(id = id, title = title, color = 0L, items = items)

    // ── openNew ───────────────────────────────────────────────────────────────

    @Test
    fun openNew_resetsEditorState() = runTest {
        // GIVEN
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)

        // WHEN
        viewModel.openNew()

        // THEN
        viewModel.editor.test {
            val state = awaitItem()
            assertNull(state.checkList)
            assertEquals("", state.title)
            assertEquals(COLOR_TRANSPARENT, state.color)
            assertTrue(state.items.isEmpty())
        }
    }

    // ── loadForEditing ────────────────────────────────────────────────────────

    @Test
    fun loadForEditing_whenChecklistExists_populatesEditorState() = runTest {
        // GIVEN
        val items = listOf(item("a", "Buy milk"), item("b", "Buy eggs", isDone = true))
        val list = checklist(id = 5, title = "Groceries", items = items)
        coEvery { repository.getLists() } returns flowOf(listOf(list))
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)

        // WHEN
        viewModel.loadForEditing(5)

        // THEN
        viewModel.editor.test {
            val state = awaitItem()
            assertEquals(list, state.checkList)
            assertEquals("Groceries", state.title)
            assertEquals(2, state.items.size)
        }
    }

    @Test
    fun loadForEditing_whenChecklistDoesNotExist_keepsEmptyState() = runTest {
        // GIVEN
        coEvery { repository.getLists() } returns flowOf(emptyList())
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)

        // WHEN
        viewModel.loadForEditing(99)

        // THEN — state should remain untouched (no crash, no phantom data)
        viewModel.editor.test {
            val state = awaitItem()
            assertNull(state.checkList)
            assertTrue(state.items.isEmpty())
        }
    }

    // ── updateTitle / updateColor ─────────────────────────────────────────────

    @Test
    fun updateTitle_updatesStateCorrectly() = runTest {
        // GIVEN
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)

        // WHEN
        viewModel.updateTitle("Shopping")

        // THEN
        viewModel.editor.test {
            assertEquals("Shopping", awaitItem().title)
        }
    }

    @Test
    fun updateColor_updatesStateCorrectly() = runTest {
        // GIVEN
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)

        // WHEN
        viewModel.updateColor(0xFF123456L)

        // THEN
        viewModel.editor.test {
            assertEquals(0xFF123456L, awaitItem().color)
        }
    }

    // ── addItem ───────────────────────────────────────────────────────────────

    @Test
    fun addItem_addsEmptyItemToList_andReturnsItsId() = runTest {
        // GIVEN
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)

        // WHEN
        val returnedId = viewModel.addItem()

        // THEN
        viewModel.editor.test {
            val state = awaitItem()
            assertEquals(1, state.items.size)
            assertEquals(returnedId, state.items[0].id)
            assertEquals("", state.items[0].text)
            assertEquals(false, state.items[0].isDone)
        }
    }

    @Test
    fun addItem_multipleItems_positionsAreSequential() = runTest {
        // GIVEN
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)

        // WHEN
        viewModel.addItem()
        viewModel.addItem()
        viewModel.addItem()

        // THEN
        viewModel.editor.test {
            val items = awaitItem().items
            assertEquals(3, items.size)
            assertEquals(0, items[0].position)
            assertEquals(1, items[1].position)
            assertEquals(2, items[2].position)
        }
    }

    // ── updateItemText ────────────────────────────────────────────────────────

    @Test
    fun updateItemText_updatesCorrectItem() = runTest {
        // GIVEN
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)
        val id = viewModel.addItem()

        // WHEN
        viewModel.updateItemText(id, "Milk")

        // THEN
        viewModel.editor.test {
            val state = awaitItem()
            assertEquals("Milk", state.items.first { it.id == id }.text)
        }
    }

    @Test
    fun updateItemText_doesNotAffectOtherItems() = runTest {
        // GIVEN
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)
        val id1 = viewModel.addItem()
        val id2 = viewModel.addItem()

        // WHEN
        viewModel.updateItemText(id1, "Item 1 text")

        // THEN
        viewModel.editor.test {
            val items = awaitItem().items
            assertEquals("Item 1 text", items.first { it.id == id1 }.text)
            assertEquals("", items.first { it.id == id2 }.text)
        }
    }

    // ── toggleItem ────────────────────────────────────────────────────────────

    @Test
    fun toggleItem_marksItemAsDone() = runTest {
        // GIVEN
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)
        val id = viewModel.addItem()

        // WHEN
        viewModel.toggleItem(id)

        // THEN
        viewModel.editor.test {
            assertEquals(true, awaitItem().items.first { it.id == id }.isDone)
        }
    }

    @Test
    fun toggleItem_togglesBackToUndone() = runTest {
        // GIVEN
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)
        val id = viewModel.addItem()
        viewModel.toggleItem(id) // first toggle → done

        // WHEN
        viewModel.toggleItem(id) // second toggle → undone

        // THEN
        viewModel.editor.test {
            assertEquals(false, awaitItem().items.first { it.id == id }.isDone)
        }
    }

    // ── removeItem ────────────────────────────────────────────────────────────

    @Test
    fun removeItem_removesCorrectItem() = runTest {
        // GIVEN
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)
        val id1 = viewModel.addItem()
        val id2 = viewModel.addItem()

        // WHEN
        viewModel.removeItem(id1)

        // THEN
        viewModel.editor.test {
            val items = awaitItem().items
            assertEquals(1, items.size)
            assertEquals(id2, items[0].id)
        }
    }

    // ── reorderPendingItems ───────────────────────────────────────────────────

    @Test
    fun reorderPendingItems_movesItemCorrectly() = runTest {
        // GIVEN — three pending items in order A, B, C
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)
        val idA = viewModel.addItem(); viewModel.updateItemText(idA, "A")
        val idB = viewModel.addItem(); viewModel.updateItemText(idB, "B")
        val idC = viewModel.addItem(); viewModel.updateItemText(idC, "C")

        // WHEN — move A (index 0) to the end (index 2) → expected: B, C, A
        viewModel.reorderPendingItems(fromIndex = 0, toIndex = 2)

        // THEN
        viewModel.editor.test {
            val pending = awaitItem().items.filter { !it.isDone }
            assertEquals("B", pending[0].text)
            assertEquals("C", pending[1].text)
            assertEquals("A", pending[2].text)
        }
    }

    @Test
    fun reorderPendingItems_doesNotAffectCompletedItems() = runTest {
        // GIVEN — two pending items and one completed
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)
        val idA = viewModel.addItem(); viewModel.updateItemText(idA, "A")
        val idB = viewModel.addItem(); viewModel.updateItemText(idB, "B")
        val idDone = viewModel.addItem(); viewModel.updateItemText(idDone, "Done"); viewModel.toggleItem(idDone)

        // WHEN — reorder pending items
        viewModel.reorderPendingItems(fromIndex = 0, toIndex = 1)

        // THEN — the completed item remains present and still isDone
        viewModel.editor.test {
            val state = awaitItem()
            val completed = state.items.filter { it.isDone }
            assertEquals(1, completed.size)
            assertEquals("Done", completed[0].text)
        }
    }

    @Test
    fun reorderPendingItems_updatesPositionsSequentially() = runTest {
        // GIVEN
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)
        val id1 = viewModel.addItem()
        val id2 = viewModel.addItem()
        val id3 = viewModel.addItem()

        // WHEN
        viewModel.reorderPendingItems(fromIndex = 2, toIndex = 0)

        // THEN — all positions must be 0, 1, 2 without gaps
        viewModel.editor.test {
            val positions = awaitItem().items.map { it.position }
            assertEquals(listOf(0, 1, 2), positions)
        }
    }

    // ── closeAndSave ──────────────────────────────────────────────────────────

    @Test
    fun closeAndSave_withNewChecklist_callsRepositoryUpsert() = runTest {
        // GIVEN
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)
        viewModel.updateTitle("Groceries")
        val id = viewModel.addItem(); viewModel.updateItemText(id, "Milk")

        // WHEN
        viewModel.closeAndSave()

        // THEN
        coVerify {
            repository.upsertList(match { list ->
                list.title == "Groceries" && list.items.any { it.text == "Milk" }
            })
        }
    }

    @Test
    fun closeAndSave_withEmptyTitleAndNoItems_doesNotCallRepository() = runTest {
        // GIVEN — blank editor, nothing to save
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)

        // WHEN
        viewModel.closeAndSave()

        // THEN
        coVerify(exactly = 0) { repository.upsertList(any()) }
    }

    @Test
    fun closeAndSave_filtersOutBlankItems_beforePersisting() = runTest {
        // GIVEN — one real item and one blank (ghost) item
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)
        viewModel.updateTitle("List")
        val idReal = viewModel.addItem(); viewModel.updateItemText(idReal, "Real item")
        viewModel.addItem() // blank item — should be discarded

        // WHEN
        viewModel.closeAndSave()

        // THEN — only the non-blank item reaches the repository
        coVerify {
            repository.upsertList(match { list ->
                list.items.size == 1 && list.items[0].text == "Real item"
            })
        }
    }

    @Test
    fun closeAndSave_withExistingChecklist_updatesItInPlace() = runTest {
        // GIVEN — editor loaded with an existing checklist
        val original = checklist(id = 7, title = "Old Title")
        coEvery { repository.getLists() } returns flowOf(listOf(original))
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)
        viewModel.loadForEditing(7)
        viewModel.updateTitle("New Title")

        // WHEN
        viewModel.closeAndSave()

        // THEN — the upsert must use the same ID with the updated title
        coVerify {
            repository.upsertList(match { list ->
                list.id == 7 && list.title == "New Title"
            })
        }
    }

    @Test
    fun closeAndSave_persistsCurrentState() = runTest {
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)
        viewModel.updateTitle("Temp")

        // WHEN
        viewModel.closeAndSave()

        coVerify {
            repository.upsertList(match { it.title == "Temp" })
        }
    }

    // ── deleteCheckList ───────────────────────────────────────────────────────

    @Test
    fun deleteCheckList_callsRepositoryDelete() = runTest {
        // GIVEN
        val list = checklist(id = 3, title = "To delete")
        coEvery { repository.getLists() } returns flowOf(listOf(list))
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)

        // WHEN
        viewModel.deleteCheckList(3)

        // THEN
        coVerify { repository.deleteList(list) }
    }

    @Test
    fun deleteCheckList_whenNotFound_doesNotCallRepository() = runTest {
        // GIVEN
        coEvery { repository.getLists() } returns flowOf(emptyList())
        viewModel = CheckListViewModel(repository, categoryRepository, userPreferencesRepository)

        // WHEN
        viewModel.deleteCheckList(99)

        // THEN
        coVerify(exactly = 0) { repository.deleteList(any()) }
    }
}