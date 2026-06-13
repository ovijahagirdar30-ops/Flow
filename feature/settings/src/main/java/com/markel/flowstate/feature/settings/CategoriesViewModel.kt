package com.markel.flowstate.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.core.domain.Category
import com.markel.flowstate.core.domain.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val categoriesEnabled: StateFlow<Boolean> = userPreferencesRepository.categoriesEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val categories: StateFlow<List<Category>> = categoryRepository.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setCategoriesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveCategoriesEnabled(enabled)
            if (enabled) {
                // Create "General" category if no categories exist yet
                val current = categoryRepository.getCategories()
                    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value
                if (current.isEmpty()) {
                    categoryRepository.upsertCategory(
                        Category(name = "General", position = 0)
                    )
                }
            }
        }
    }

    fun createCategory(name: String) {
        viewModelScope.launch {
            val currentList = categories.value
            val maxPosition = currentList.maxOfOrNull { it.position } ?: -1
            categoryRepository.upsertCategory(
                Category(name = name, position = maxPosition + 1)
            )
        }
    }

    fun deleteCategory(id: Int, deleteItems: Boolean) {
        viewModelScope.launch {
            if (deleteItems) {
                categoryRepository.deleteItemsInCategory(id)
            } else {
                categoryRepository.moveItemsToGeneral(id)
            }
            categoryRepository.deleteCategory(id)
        }
    }

    fun reorderCategories(categories: List<Category>) {
        viewModelScope.launch {
            val reordered = categories.mapIndexed { index, category ->
                category.copy(position = index)
            }
            categoryRepository.updateCategoriesOrder(reordered)
        }
    }
}
