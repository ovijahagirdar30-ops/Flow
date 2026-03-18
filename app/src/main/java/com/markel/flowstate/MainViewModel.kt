package com.markel.flowstate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.lastTabRoute.collect { route ->
                // If there isn't a saved route, go to Tasks by default
                _startDestination.value = route ?: Screen.Tasks.route
            }
        }
    }

    fun saveLastTab(route: String) {
        viewModelScope.launch {
            userPreferencesRepository.saveLastTabRoute(route)
        }
    }
}