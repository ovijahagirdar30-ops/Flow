package com.markel.flowstate.core.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialShapes.Companion.Pill
import androidx.compose.material3.MaterialShapes.Companion.SoftBurst
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.markel.flowstate.core.designsystem.R as DesignR
import com.markel.flowstate.core.domain.Habit
import com.markel.flowstate.core.domain.HabitRepository
import com.markel.flowstate.core.domain.HabitType
import com.markel.flowstate.core.designsystem.icon.HabitIconMapper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import androidx.compose.ui.platform.LocalLocale

/**
 * Holds a habit together with its completion entries (as epochDays)
 * so the picker can show completion status and week-day dots.
 */
private data class HabitPickerData(
    val habit: Habit,
    val completedDates: Set<Long> // epochDay values for fast lookup
) {
    val isCompletedToday: Boolean
        get() = LocalDate.now().toEpochDay() in completedDates
}

@AndroidEntryPoint
class HabitWidgetConfigActivity : ComponentActivity() {

    @Inject lateinit var habitRepository: HabitRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Default value if the user cancels
        setResult(Activity.RESULT_CANCELED)

        val appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            val scope = rememberCoroutineScope()
            var habits by remember { mutableStateOf<List<HabitPickerData>>(emptyList()) }

            val darkTheme = isSystemInDarkTheme()
            val context = LocalContext.current

            val colorScheme = remember(darkTheme) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            LaunchedEffect(Unit) {
                val booleanHabits = habitRepository.getHabits().first()
                    .filter { it.habitType == HabitType.BOOLEAN }
                val pickerData = booleanHabits.map { habit ->
                    val entries = habitRepository.getEntriesForHabit(habit.id).first()
                    HabitPickerData(
                        habit = habit,
                        completedDates = entries.map { it.toEpochDay() }.toSet()
                    )
                }
                habits = pickerData
            }

            MaterialTheme(colorScheme = colorScheme) {
                HabitPickerScreen(
                    habits = habits,
                    onHabitSelected = { pickerData ->
                        val habit = pickerData.habit
                        scope.launch {
                            val glanceId = GlanceAppWidgetManager(this@HabitWidgetConfigActivity)
                                .getGlanceIdBy(appWidgetId)

                            updateAppWidgetState(
                                this@HabitWidgetConfigActivity,
                                PreferencesGlanceStateDefinition,
                                glanceId
                            ) { prefs ->
                                prefs.toMutablePreferences().apply {
                                    this[KEY_HABIT_ID] = habit.id
                                }
                            }

                            try {
                                HabitWidget().update(this@HabitWidgetConfigActivity, glanceId)
                            } catch (_: IllegalStateException) {
                                // Session not available yet — will be created on next onUpdate()
                            }

                            // Return ok to the launcher
                            setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            )
                            finish()
                        }
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitPickerScreen(
    habits: List<HabitPickerData>,
    onHabitSelected: (HabitPickerData) -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.choose_habit),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.padding(end = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )

            if (habits.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🌱",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_boolean_habits),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(habits, key = { it.habit.id }) { pickerData ->
                        HabitPickerItem(
                            pickerData = pickerData,
                            onClick = { onHabitSelected(pickerData) }
                        )
                    }

                    // Bottom spacing
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

/**
 * Individual habit item in the picker.
 *
 * - When the habit is completed today, the icon circle becomes solid.
 * - A compact row of 7 dots shows which days of the current week are completed.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HabitPickerItem(
    pickerData: HabitPickerData,
    onClick: () -> Unit
) {
    val habit = pickerData.habit
    val habitColor = Color(habit.colorArgb)
    val isCompleted = pickerData.isCompletedToday

    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val cardBg by animateColorAsState(
        targetValue = if (isCompleted)
            habitColor.copy(alpha = 0.12f).compositeOver(surfaceColor)
        else
            surfaceColor,
        animationSpec = tween(300),
        label = "card_bg"
    )

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), // Unified padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Icon ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(if (isCompleted) SoftBurst.toShape() else Pill.toShape())
                    .background(
                        if (isCompleted) habitColor
                        else habitColor.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val iconRes = HabitIconMapper.getDrawableRes(habit.iconName)
                if (iconRes != null) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(iconRes),
                        contentDescription = null,
                        tint = if (isCompleted) Color.White else habitColor,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = habit.name.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) Color.White else habitColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // ── Name + Dots ──────────────────────────────
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                WeekProgressText(
                    completedDates = pickerData.completedDates,
                    habitColor = habitColor
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                painter = androidx.compose.ui.res.painterResource(DesignR.drawable.add_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun WeekProgressText(
    completedDates: Set<Long>,
    habitColor: Color
) {
    val today = LocalDate.now()
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDates = (0L..6L).map { monday.plusDays(it) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        weekDates.forEach { date ->
            val isDone = date.toEpochDay() in completedDates
            val isToday = date == today
            val isFuture = date.isAfter(today)

            val dayInitial = date.dayOfWeek
                .getDisplayName(java.time.format.TextStyle.NARROW, LocalLocale.current.platformLocale)
                .uppercase()

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isDone -> habitColor
                            isFuture -> Color.Transparent
                            isToday && !isDone -> habitColor.copy(alpha = 0.25f)
                            else -> habitColor.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayInitial,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 12.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        isDone -> Color.White
                        isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        isToday -> habitColor
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}