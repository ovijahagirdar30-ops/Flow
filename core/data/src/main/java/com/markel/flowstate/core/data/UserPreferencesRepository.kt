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
    val isGridView: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[IS_GRID_VIEW] ?: false }
    val lastTabRoute: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[stringPreferencesKey("last_tab_route")]
    }
    private val CALENDAR_VIEW_MODE = stringPreferencesKey("calendar_view_mode")

    val calendarViewMode: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[CALENDAR_VIEW_MODE]
    }

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

    suspend fun saveCalendarViewMode(mode: String) {
        context.dataStore.edit { it[CALENDAR_VIEW_MODE] = mode }
    }
}