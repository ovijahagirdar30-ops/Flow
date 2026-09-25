package com.markel.flowstate.feature.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markel.flowstate.core.domain.DayReview
import com.markel.flowstate.core.domain.Task
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val AccentPurple = Color(0xFF9C27B0)
private val DividerGray = Color(0xFF424242)

private val reviewDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
private val dueDateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

/**
 * The 9PM night check-in: one page reviewing the day — tasks completed
 * today, what's still open, what rolled to tomorrow — closing with the
 * encouraging line from the [com.markel.flowstate.core.domain.EncouragementGenerator]
 * seam. Reached only from CheckinActivity in night-review mode (the 9PM
 * alarm, or the debug openNight hook); not a bottom-nav destination.
 *
 * Every Text carries an explicit color — nothing provides LocalContentColor
 * in this app, so the default would be black-on-black (the Mood-header bug).
 */
@Composable
fun NightReviewScreen(
    onDone: () -> Unit,
    viewModel: NightReviewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val review = state.review

    if (review == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AccentPurple)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = formatDate(review.date),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Night check-in",
            style = MaterialTheme.typography.headlineSmall,
            color = AccentPurple
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Completed today ────────────────────────────────────────────────
        ReviewHeader(title = "Completed today", count = review.completedToday.size)
        if (review.completedToday.isEmpty()) {
            Text(
                text = "No checkmarks today — showing up still counts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            review.completedToday.forEach { task ->
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = TextDecoration.LineThrough,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        HorizontalDivider(color = DividerGray)

        // ── Still open ─────────────────────────────────────────────────────
        ReviewHeader(title = "Still open", count = review.pending.size)
        if (review.pending.isEmpty()) {
            Text(
                text = "Nothing left open. Enjoy the evening.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            review.pending.forEach { task -> PendingRow(task) }
        }
        HorizontalDivider(color = DividerGray)

        // ── Pushed to tomorrow (hidden when there's nothing to push) ───────
        if (review.pushedToTomorrow.isNotEmpty()) {
            ReviewHeader(title = "Pushed to tomorrow", count = review.pushedToTomorrow.size)
            review.pushedToTomorrow.forEach { task -> PendingRow(task) }
            HorizontalDivider(color = DividerGray)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Encouraging close ──────────────────────────────────────────────
        val message = state.message
        if (message == null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    color = AccentPurple,
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Thinking about your day…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = AccentPurple,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
        ) {
            Text("Done for today", color = Color.White)
        }
    }
}

@Composable
private fun ReviewHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PendingRow(task: Task) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        task.dueDate?.let { due ->
            Text(
                text = "due ${formatDueDate(due)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(isoDate: String): String = runCatching {
    LocalDate.parse(isoDate).format(reviewDateFormatter)
}.getOrDefault(isoDate)

private fun formatDueDate(millis: Long): String = runCatching {
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(dueDateFormatter)
}.getOrDefault("")
