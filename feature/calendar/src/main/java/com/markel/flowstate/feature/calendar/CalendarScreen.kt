package com.markel.flowstate.feature.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.markel.flowstate.feature.calendar.components.CalendarContent

@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()) {
        when (val state = uiState) {
            is CalendarUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is CalendarUiState.Success -> {
                CalendarContent(
                    tasksByDate = state.tasksByDate,
                    selectedDate = state.selectedDate,
                    onDateSelected = viewModel::onDateSelected,
                    onTaskToggle = { task ->
                        viewModel.toggleTaskDone(task)
                    },
                )
            }
        }
    }
}