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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.markel.flowstate.core.domain.checkin.UnexpectedPlan

@Composable
fun UnexpectedPlansStep(
    plans: List<UnexpectedPlan>,
    onAddPlan: (UnexpectedPlan) -> Unit,
    onRemovePlan: (UnexpectedPlan) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var description by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Any unexpected plans tonight?",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )

        plans.forEach { plan ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(plan.description, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    Text(
                        "${plan.startTime} \u00b7 ${plan.durationMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                TextButton(
                    onClick = { onRemovePlan(plan) },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF9C27B0))
                ) {
                    Text("Remove")
                }
            }
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("What's going on? (e.g. Dinner with family)", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF9C27B0),
                unfocusedBorderColor = Color(0xFF616161),
                cursorColor = Color(0xFF9C27B0)
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = startTime,
                onValueChange = { startTime = it },
                placeholder = { Text("Time (e.g. 7:00 PM)", color = Color.Gray) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF9C27B0),
                    unfocusedBorderColor = Color(0xFF616161),
                    cursorColor = Color(0xFF9C27B0)
                )
            )
            OutlinedTextField(
                value = duration,
                onValueChange = { input -> duration = input.filter { it.isDigit() } },
                placeholder = { Text("Minutes", color = Color.Gray) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF9C27B0),
                    unfocusedBorderColor = Color(0xFF616161),
                    cursorColor = Color(0xFF9C27B0)
                )
            )
        }
        OutlinedButton(
            onClick = {
                if (description.isNotBlank()) {
                    onAddPlan(
                        UnexpectedPlan(
                            description = description,
                            startTime = startTime,
                            durationMinutes = duration.toIntOrNull() ?: 0
                        )
                    )
                    description = ""
                    startTime = ""
                    duration = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9C27B0))
        ) {
            Text("Add plan")
        }

        Text(
            text = "Additional comments",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            placeholder = { Text("Anything else on your mind?", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF9C27B0),
                unfocusedBorderColor = Color(0xFF616161),
                cursorColor = Color(0xFF9C27B0)
            )
        )

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
        ) {
            Text("Next", color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
