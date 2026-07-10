package com.markel.flowstate.feature.habits.details

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import com.markel.flowstate.core.designsystem.R as DesignR
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import com.markel.flowstate.feature.habits.R
import com.markel.flowstate.feature.habits.details.components.numeric.NumericEvolutionCard
import com.markel.flowstate.feature.habits.details.components.bool.HabitMonthCalendar
import com.markel.flowstate.feature.habits.details.components.bool.RadarChart
import com.markel.flowstate.feature.habits.details.components.SectionHeader
import com.markel.flowstate.feature.habits.details.components.bool.StatCard
import com.markel.flowstate.feature.habits.details.components.bool.WeeklyBarsCard
import com.markel.flowstate.feature.habits.details.components.numeric.MonthlyGoalCard
import com.markel.flowstate.feature.habits.details.components.numeric.NumericHeatmapCard
import com.markel.flowstate.feature.habits.details.components.numeric.ValueDistributionCard
import com.markel.flowstate.feature.habits.util.formatFloat

@Composable
fun HabitDetailScreen(
    habitId: Int,
    onBack: () -> Unit,
    viewModel: HabitDetailViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val habit = state.habit ?: return
    val habitColor = Color(habit.colorArgb)
    val locale = LocalLocale.current.platformLocale

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Top bar ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(DesignR.drawable.arrow_back_24px),
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // ── Hero ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(habitColor)
            )
            Text(
                text = habit.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        val entriesCount = if (state.isNumeric) state.numericEntries.size else state.allEntries.size
        Text(
            text = stringResource(
                R.string.habit_detail_since,
                habit.createdAt.toString(),
                entriesCount
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 44.dp, bottom = 20.dp)
        )

        // ── Metrics ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                value = state.currentStreak.toString(),
                label = stringResource(R.string.habit_detail_streak_current),
                valueColor = habitColor,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = state.bestStreak.toString(),
                label = stringResource(R.string.habit_detail_streak_best),
                modifier = Modifier.weight(1f)
            )

            if (!state.isNumeric) {
                StatCard(
                    value = state.completionPct(),
                    label = state.pctLabel(),
                    modifier = Modifier.weight(1f)
                )
            } else {
                val avgValue = state.monthlyProgress?.dailyAverage ?: 0f
                StatCard(
                    value = formatFloat(avgValue) + " " + (habit.unit ?: ""),
                    label = stringResource(R.string.habit_detail_average),
                    modifier = Modifier.weight(1f)
                )
            }

        }

        if (state.isNumeric) {
            // Evolution "line chart"
            SectionHeader(title = stringResource(R.string.habit_detail_evolution))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                NumericEvolutionCard(
                    dailyValues = state.dailyValues,
                    targetValue = habit.targetValue,
                    habitColor = habitColor,
                    unit = habit.unit,
                    modifier = Modifier.padding(16.dp)
                )
            }

            SectionHeader(title = stringResource(R.string.habit_heatmap))
            // Heatmap
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                NumericHeatmapCard(
                    heatmapData = state.heatmapData,
                    targetValue = habit.targetValue,
                    habitColor = habitColor,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Monthly goal
            if (habit.targetValue != null) {
                SectionHeader(title = stringResource(R.string.habit_monthly_goal))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    MonthlyGoalCard(
                        progress = state.monthlyProgress,
                        habitColor = habitColor,
                        unit = habit.unit,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            SectionHeader(title = stringResource(R.string.habit_detail_average))
            // Weekly averages
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                ValueDistributionCard(
                    distribution = state.dayOfWeekAverages,
                    habitColor = habitColor,
                    unit = habit.unit,
                    modifier = Modifier.padding(16.dp)
                )
            }

        }
        else {

            // ── History ─────────────────────────────────────────────
            SectionHeader(
                title = stringResource(R.string.habit_detail_section_history),
                actionLabel = state.viewMode.label(),
                onAction = { viewModel.cycleViewMode() }
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                // Navigation arrows
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { viewModel.navigatePrevious() }) {
                        Text("‹", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        text = state.navigationLabel(locale),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = { viewModel.navigateNext() }) {
                        Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                AnimatedContent(
                    targetState = state.viewMode,
                    transitionSpec = {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                    },
                    label = "calendar_view"
                ) { mode ->
                    when (mode) {
                        CalendarViewMode.ONE_MONTH -> {
                            HabitMonthCalendar(
                                year = state.displayYear,
                                month = state.displayMonth,
                                completedEpochDays = state.allEntries,
                                habitColor = habitColor,
                                modifier = Modifier.padding(
                                    start = 12.dp, end = 12.dp, bottom = 12.dp
                                )
                            )
                        }

                        CalendarViewMode.THREE_MONTHS -> {
                            Row(
                                modifier = Modifier.padding(
                                    start = 8.dp, end = 8.dp, bottom = 12.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                (2 downTo 0).forEach { monthsAgo ->
                                    var m = state.displayMonth - monthsAgo
                                    var y = state.displayYear
                                    if (m < 0) {
                                        m += 12; y--
                                    }
                                    HabitMonthCalendar(
                                        year = y, month = m,
                                        completedEpochDays = state.allEntries,
                                        habitColor = habitColor,
                                        showMonthLabel = true,
                                        compact = false,
                                        showNumbers = false,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        CalendarViewMode.ONE_YEAR -> {
                            Column(
                                modifier = Modifier.padding(
                                    start = 12.dp, end = 12.dp, bottom = 12.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                (0..11).chunked(4).forEach { rowMonths ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowMonths.forEach { m ->
                                            HabitMonthCalendar(
                                                year = state.displayYear,
                                                month = m,
                                                completedEpochDays = state.allEntries,
                                                habitColor = habitColor,
                                                showMonthLabel = true,
                                                compact = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Weekly Bars ──────────────────────────────────────
            val selectedWeekData = run {
                val data = when (state.weeklyBarsMode) {
                    WeeklyBarsMode.EIGHT -> state.weeklyCompletions.takeLast(8)
                    WeeklyBarsMode.SIXTEEN -> state.weeklyCompletions
                }
                val idx = state.selectedBarIndex ?: (data.size - 1)
                data.getOrNull(idx)
            }

            val weekLabel = selectedWeekData?.first?.let { weekStart ->
                val now = LocalDate.now().with(java.time.DayOfWeek.MONDAY)
                if (weekStart == now) stringResource(R.string.habit_detail_this_week)
                else "${weekStart.dayOfMonth} ${
                    weekStart.month
                        .getDisplayName(
                            java.time.format.TextStyle.SHORT,
                            LocalLocale.current.platformLocale
                        )
                }"
            }

            SectionHeader(
                title =
                    if (weekLabel != null)
                        stringResource(R.string.habit_detail_section_weekly, weekLabel)
                    else
                        stringResource(R.string.habit_detail_section_weekly_empty)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                WeeklyBarsCard(
                    weeklyCompletions = state.weeklyCompletions,
                    selectedIndex = state.selectedBarIndex,
                    barsMode = state.weeklyBarsMode,
                    habitColor = habitColor,
                    onBarSelected = { viewModel.selectBar(it) },
                    onModeChanged = { viewModel.setWeeklyBarsMode(it) },
                    modifier = Modifier.padding(16.dp)
                )
            }

            // ── Radar chart ───────────────────────────────────────────────
            SectionHeader(title = stringResource(R.string.habit_detail_section_radar))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    RadarChart(
                        dayOfWeekCompletions = state.dayOfWeekCompletions,
                        habitColor = habitColor
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}