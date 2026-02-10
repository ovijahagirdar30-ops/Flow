package com.markel.flowstate.feature.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.markel.flowstate.feature.calendar.components.CalendarContent
import kotlinx.coroutines.launch

@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val message = stringResource(R.string.toggle)
    val uMessage = stringResource(R.string.undo)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                                scope.launch {
                                    // Hide any previous snackbar
                                    snackbarHostState.currentSnackbarData?.dismiss()

                                    val result = snackbarHostState.showSnackbar(
                                        message = message,
                                        actionLabel = uMessage,
                                        duration = SnackbarDuration.Short
                                    )

                                    // If "undo" is pressed
                                    if (result == SnackbarResult.ActionPerformed) {
                                        // We need to use a different function, because now the task variable is obsolete, so we need to force the done state of the task with this function
                                        viewModel.undoTaskToggle(task)
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}