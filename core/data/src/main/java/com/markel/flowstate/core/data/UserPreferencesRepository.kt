package com.markel.flowstate.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val IS_GRID_VIEW = booleanPreferencesKey("is_grid_view")
    val lastTabRoute: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[stringPreferencesKey("last_tab_route")]
    }

    val isGridView: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[IS_GRID_VIEW] ?: false }

    suspend fun setGridView(isGrid: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_GRID_VIEW] = isGrid
        }
    }

    suspend fun saveLastTabRoute(route: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey("last_tab_route")] = route
        }
    }
}