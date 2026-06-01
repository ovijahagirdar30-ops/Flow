package com.markel.flowstate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.data.MainTab
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _startDestination = MutableStateFlow<Any?>(null)
    val startDestination = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.lastTab.collect { tab ->
                _startDestination.value = tab.toRoute()
            }
        }
    }

    fun saveLastTab(tab: MainTab) {
        viewModelScope.launch {
            userPreferencesRepository.saveLastTab(tab)
        }
    }
}