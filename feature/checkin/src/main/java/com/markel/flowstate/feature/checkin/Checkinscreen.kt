package com.markel.flowstate.feature.checkin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markel.flowstate.core.domain.CheckinItem
import com.markel.flowstate.core.domain.CheckinItemType
import kotlinx.coroutines.launch

@Composable
fun CheckinScreen(
    onDismiss: () -> Unit,
    onOpenPlan: () -> Unit,
    viewModel: CheckinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val endOfDayMinutes by viewModel.endOfDayMinutes.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    when (val state = uiState) {
        is CheckinUiState.Loading -> Unit

        is CheckinUiState.InProgress -> when (state.step) {
            CheckinStep.MOOD -> MoodCheckinStep(
                moodState = state.mood,
                onEnergyChange = viewModel::updateEnergy,
                onSleepinessChange = viewModel::updateSleepiness,
                onStressChange = viewModel::updateStress,
                onHeadacheChange = viewModel::updateHeadache,
                onMotivationChange = viewModel::updateMotivation,
                onEnergyCommentChange = viewModel::updateEnergyComment,
                onSleepinessCommentChange = viewModel::updateSleepinessComment,
                onStressCommentChange = viewModel::updateStressComment,
                onHeadacheCommentChange = viewModel::updateHeadacheComment,
                onMotivationCommentChange = viewModel::updateMotivationComment,
                endOfDayMinutes = endOfDayMinutes,
                onEndOfDayChange = viewModel::setEndOfDayMinutes,
                onNext = viewModel::goToNextStep
            )

            CheckinStep.UNEXPECTED_PLANS -> UnexpectedPlansStep(
                plans = state.unexpectedPlans,
                onAddPlan = viewModel::addUnexpectedPlan,
                onRemovePlan = viewModel::removeUnexpectedPlan,
                onNext = viewModel::goToNextStep
            )

            CheckinStep.TASKS -> TasksCheckinStep(
                items = state.items,
                isPlanning = state.isPlanning,
                onAddTask = viewModel::addTask,
                onGeneratePlan = viewModel::generatePlan
            )

            CheckinStep.PLAN -> PlanCheckinStep(
                plan = state.plan,
                isPlanning = state.isPlanning,
                onRegenerate = viewModel::regeneratePlan,
                onAgree = {
                    scope.launch {
                        viewModel.agreeToPlan()
                        onOpenPlan()
                    }
                },
                onDiscard = onDismiss
            )
        }
    }
}

@Composable
private fun TasksCheckinStep(
    items: List<CheckinItem>,
    isPlanning: Boolean,
    onAddTask: (String) -> Unit,
    onGeneratePlan: () -> Unit
) {
    var newTaskTitle by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Today's tasks",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick-add: lands in the same tasks table the FlowState Tasks
        // screen reads, so it appears there alongside manual tasks.
        OutlinedTextField(
            value = newTaskTitle,
            onValueChange = { newTaskTitle = it },
            placeholder = { Text("Add a task for tonight...", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF9C27B0),
                unfocusedBorderColor = Color(0xFF616161),
                cursorColor = Color(0xFF9C27B0)
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (newTaskTitle.isNotBlank()) {
                    onAddTask(newTaskTitle)
                    newTaskTitle = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
        ) {
            Text("Add task", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nothing left for today \u2014 nice.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items) { item ->
                    CheckinItemRow(item)
                    HorizontalDivider(color = Color(0xFF424242))
                }
            }
        }

        Button(
            onClick = onGeneratePlan,
            enabled = !isPlanning,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
        ) {
            Text(if (isPlanning) "Planning your evening..." else "Show my plan", color = Color.White)
        }
    }
}

@Composable
private fun CheckinItemRow(item: CheckinItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(text = item.title, style = MaterialTheme.typography.bodyLarge, color = Color.White)
        Text(
            text = if (item.type == CheckinItemType.TASK) "Task" else "Habit",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
