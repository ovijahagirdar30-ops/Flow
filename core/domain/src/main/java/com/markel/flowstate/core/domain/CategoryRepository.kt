package com.markel.flowstate.core.domain

import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: Int): Category?
    suspend fun upsertCategory(category: Category): Long
    suspend fun deleteCategory(id: Int)
    suspend fun updateCategoriesOrder(categories: List<Category>)
    suspend fun moveItemsToGeneral(categoryId: Int)
    suspend fun deleteItemsInCategory(categoryId: Int)
}