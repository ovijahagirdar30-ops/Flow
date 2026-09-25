package com.markel.flowstate.feature.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markel.flowstate.core.domain.EveningPlan
import com.markel.flowstate.core.domain.PlanBlock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The Plan checklist tab: tonight's most recently AGREED evening plan with a
 * checkbox per block, plus the headline, date, and a done counter. Ticks are
 * persisted by [PlanViewModel] (survive restarts); ticking a TASK block marks
 * the real task done, habit/free blocks tick visually only.
 *
 * After the user's end-of-day time (or at midnight, the default cutoff) the
 * plan ages out — [PlanUiState.isExpired] — and the tab shows the same empty
 * state as a never-agreed evening, so yesterday's plan never lingers.
 *
 * Deliberately theme-aware (unlike the black check-in screens): this renders
 * inside the main app's FlowStateTheme, so it follows light/dark and the
 * user's chosen app color.
 */
@Composable
fun PlanScreen(
    viewModel: PlanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val plan = state.plan

    when {
        state.isLoading -> Unit

        plan == null || state.isExpired -> Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No plan yet — run the evening check-in and tap Agree to save one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        else -> PlanContent(
            plan = plan,
            checkedIndexes = state.checkedIndexes,
            onToggle = viewModel::toggleBlock
        )
    }
}

@Composable
private fun PlanContent(
    plan: EveningPlan,
    checkedIndexes: Set<Int>,
    onToggle: (Int) -> Unit
) {
    val doneCount = checkedIndexes.count { it in plan.blocks.indices }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = formatPlanDate(plan.date),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = plan.headline,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "$doneCount of ${plan.blocks.size} done",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        plan.blocks.forEachIndexed { index, block ->
            PlanBlockRow(
                block = block,
                checked = index in checkedIndexes,
                onToggle = { onToggle(index) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun PlanBlockRow(
    block: PlanBlock,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 12.dp, bottom = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatPlanTime(block.startTime),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${block.kind.name.lowercase().replaceFirstChar { it.uppercase() }} · ${block.durationMinutes} min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = block.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textDecoration = if (checked) TextDecoration.LineThrough else null
            )
            Text(
                text = block.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** "yyyy-MM-dd" → "Thu, 24 Sep"; falls back to the raw string if parsing ever fails. */
private fun formatPlanDate(isoDate: String): String = runCatching {
    LocalDate.parse(isoDate)
        .format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
}.getOrDefault(isoDate)
