package com.markel.flowstate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.data.MainTab
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _startDestination = MutableStateFlow<Any?>(null)
    val startDestination = _startDestination.asStateFlow()

    /** Current bottom nav order (all tabs in user-configured order). */
    val bottomNavOrder: StateFlow<List<MainTab>> = userPreferencesRepository.bottomNavOrder
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainTab.DEFAULT_ORDER)

    /** Current set of hidden tabs. */
    val bottomNavHidden: StateFlow<Set<MainTab>> = userPreferencesRepository.bottomNavHidden
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    init {
        viewModelScope.launch {
            // Combine last tab with hidden tabs to ensure startDestination is always visible
            combine(
                userPreferencesRepository.lastTab,
                userPreferencesRepository.bottomNavHidden
            ) { lastTab, hiddenTabs ->
                if (lastTab in hiddenTabs) {
                    // Fallback to the first non-hidden tab
                    val visibleTabs = MainTab.DEFAULT_ORDER.filter { it !in hiddenTabs }
                    visibleTabs.firstOrNull() ?: MainTab.TASKS
                } else {
                    lastTab
                }
            }.collect { tab ->
                _startDestination.value = tab.toRoute()
            }
        }
    }

    fun saveLastTab(tab: MainTab) {
        viewModelScope.launch {
            userPreferencesRepository.saveLastTab(tab)
        }
    }

    fun saveBottomNavConfig(order: List<MainTab>, hidden: Set<MainTab>) {
        viewModelScope.launch {
            userPreferencesRepository.saveBottomNavConfig(order, hidden)
        }
    }

}