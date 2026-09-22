package com.markel.flowstate.feature.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.markel.flowstate.core.domain.EveningPlan
import com.markel.flowstate.core.domain.PlanBlock

private val AccentPurple = Color(0xFF9C27B0)
private val LightPurple = Color(0xFFCE93D8)
private val DividerGray = Color(0xFF424242)

/**
 * Final check-in step: renders the generated evening plan (headline +
 * time-ordered blocks) and closes the popup. Backed by whatever
 * EveningPlanner is bound — LocalEveningPlanner today, Gemini next stage.
 */
@Composable
fun PlanCheckinStep(
    plan: EveningPlan?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Your evening plan",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        if (plan == null) {
            Text(
                text = "No plan was generated.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        } else {
            Text(
                text = plan.headline,
                style = MaterialTheme.typography.titleMedium,
                color = LightPurple
            )

            Spacer(modifier = Modifier.height(4.dp))

            plan.blocks.forEach { block ->
                PlanBlockRow(block)
                HorizontalDivider(color = DividerGray)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
        ) {
            Text("Close", color = Color.White)
        }
    }
}

@Composable
private fun PlanBlockRow(block: PlanBlock) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = block.startTime,
            style = MaterialTheme.typography.titleMedium,
            color = AccentPurple
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = block.title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Text(
                text = block.reason,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Text(
                text = "${block.kind.name.lowercase().replaceFirstChar { it.uppercase() }} · ${block.durationMinutes} min",
                style = MaterialTheme.typography.labelSmall,
                color = LightPurple
            )
        }
    }
}
