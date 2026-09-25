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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
 * time-ordered blocks) and collects the user's verdict:
 *  - Regenerate — the optional comment plus this plan go back into the
 *    planner (Gemini revises, Local ignores feedback); the result replaces
 *    the plan on screen.
 *  - Agree — persists the plan (the only evening_plans write) and hands off
 *    to the Plan checklist tab.
 *  - Not tonight — closes without persisting anything.
 * [isPlanning] swaps the controls for a planning indicator while the
 * planner runs (instant with Local, a few seconds with Gemini).
 */
@Composable
fun PlanCheckinStep(
    plan: EveningPlan?,
    isPlanning: Boolean,
    onRegenerate: (comment: String) -> Unit,
    onAgree: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var comment by rememberSaveable { mutableStateOf("") }

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

        if (isPlanning) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    color = AccentPurple,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Planning your evening...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        } else {
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                placeholder = { Text("What should change? (optional)", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = Color(0xFF616161),
                    cursorColor = AccentPurple
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onRegenerate(comment)
                        comment = ""
                    },
                    enabled = plan != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Regenerate", color = AccentPurple)
                }

                Button(
                    onClick = onAgree,
                    enabled = plan != null,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Text("Agree", color = Color.White)
                }
            }

            TextButton(onClick = onDiscard, modifier = Modifier.fillMaxWidth()) {
                Text("Not tonight", color = Color.Gray)
            }
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
