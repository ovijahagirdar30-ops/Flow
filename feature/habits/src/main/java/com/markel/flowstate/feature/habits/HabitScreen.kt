package com.markel.flowstate.feature.habits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markel.flowstate.feature.habits.components.AddHabitDialog
import com.markel.flowstate.feature.habits.components.HabitCard
import com.markel.flowstate.feature.habits.components.HabitEmptyState
import com.markel.flowstate.feature.habits.details.components.HabitHeader
import com.markel.flowstate.feature.habits.details.components.MotivationalMessage
import com.markel.flowstate.core.designsystem.R as DesignR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    viewModel: HabitViewModel = hiltViewModel(),
    onNavigateToDetail: (habitId: Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        when (val state = uiState) {
            is HabitUiState.Loading -> Unit

            is HabitUiState.Success -> {
                val keywords = stringArrayResource(R.array.motivational_keywords)
                val rests = stringArrayResource(R.array.motivational_rests)
                val message = MotivationalMessage(
                    keyword = keywords[state.motivationalMessageIndex],
                    rest = rests[state.motivationalMessageIndex]
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    HabitHeader(
                        completedToday = state.completedToday,
                        totalHabits = state.totalHabits,
                        motivationalMessage = message
                    )
                    if (state.habits.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            HabitEmptyState()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.habits, key = { it.habit.id }) { habitWithStatus ->
                                HabitCard(
                                    habitWithStatus = habitWithStatus,
                                    weekEntries = state.weekEntriesByHabit[habitWithStatus.habit.id]
                                        ?: emptySet(),
                                    onToggleDay = { date ->
                                        viewModel.toggleHabitOnDate(habitWithStatus.habit.id, date)
                                    },
                                    onDelete = { viewModel.deleteHabit(habitWithStatus.habit) },
                                    onEdit = { name, icon, colorArgb -> viewModel.editHabit(habitWithStatus.habit, name, icon,colorArgb) },
                                    onNavigateToDetail = { onNavigateToDetail(habitWithStatus.habit.id) }
                                )
                            }
                        }
                    }
                }

                if (state.showAddDialog) {
                    AddHabitDialog(
                        onDismiss = { viewModel.hideAddDialog() },
                        onConfirm = { name, icon, color -> viewModel.addHabit(name, icon, color) }
                    )
                }

                FloatingActionButton(
                    onClick = { viewModel.showAddDialog() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(DesignR.drawable.add_24px),
                        contentDescription = "Add habit"
                    )
                }
            }
        }
    }
}