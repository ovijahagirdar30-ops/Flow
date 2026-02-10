package com.markel.flowstate.feature.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.feature.calendar.components.AnimatedUndoFab
import com.markel.flowstate.feature.calendar.components.CalendarContent
import kotlinx.coroutines.delay

@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    var showUndoButton by remember { mutableStateOf(false) }
    var pendingUndoTask by remember { mutableStateOf<Task?>(null) }

    LaunchedEffect(showUndoButton) {
        if (showUndoButton) {
            delay(3500L)
            showUndoButton = false
            pendingUndoTask = null
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
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
                                val isTaskDisappearing = task.dueDate == null && task.isDone

                                viewModel.toggleTaskDone(task)

                                if (isTaskDisappearing) {
                                    pendingUndoTask = task
                                    showUndoButton = true

                                }
                            }
                        )
                    }
                }
            }

            AnimatedUndoFab(
                visible = showUndoButton,
                onUndoClick = {
                    pendingUndoTask?.let { task ->
                        viewModel.undoTaskToggle(task)
                        showUndoButton = false
                        pendingUndoTask = null
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
            )

        }
    }
}