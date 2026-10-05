package com.markel.flowstate.feature.checkin

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markel.flowstate.core.domain.checkin.CheckinMoodState
import kotlinx.coroutines.launch

/**
 * Evening check-in. The first seven steps are the redesigned flow — a fixed
 * header (back + five progress segments), a fixed bottom pill button and the
 * steps crossfading between them, exactly like the design. The final PLAN
 * step is the "Planning to Plan" redesign: it owns its own chrome (planning
 * loading screen, settled timeline, Agree pill) so the flow chrome fades out
 * once the recap hands off to it.
 */
@Composable
fun CheckinScreen(
    onDismiss: () -> Unit,
    onOpenPlan: () -> Unit,
    startAtPlan: Boolean = false,
    viewModel: CheckinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // "Edit plan" start: read the agreed plan BEFORE the first frame renders,
    // so the screen never flashes the greeting on its way to the plan step.
    var planEditReady by remember { mutableStateOf(!startAtPlan) }
    LaunchedEffect(startAtPlan) {
        if (startAtPlan) {
            viewModel.loadAgreedPlanForEditing()
            planEditReady = true
        }
    }

    when (val state = if (planEditReady) uiState else CheckinUiState.Loading) {
        is CheckinUiState.Loading -> Unit

        is CheckinUiState.InProgress -> {
            val step = state.step
            val inFlow = step.isDesignedFlow
            val density = LocalDensity.current.density

            // Header back arrow mirrors the system back gesture on questions.
            BackHandler(enabled = inFlow && step != CheckinStep.GREET) {
                viewModel.goToPreviousStep()
            }

            // Chrome fades out on the greeting and away entirely once the
            // plan step takes over (its own screens carry the chrome).
            val chromeAlpha by animateFloatAsState(
                targetValue = if (inFlow && step != CheckinStep.GREET) 1f else 0f,
                animationSpec = tween(500),
                label = "checkinChrome"
            )
            // Which note fields are open — survives step hops within the flow.
            var openNotes by remember { mutableStateOf(setOf<CheckinStep>()) }

            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(visible = inFlow) {
                    CheckinFlowHeader(
                        currentStep = step,
                        alpha = chromeAlpha,
                        onBack = viewModel::goToPreviousStep
                    )
                }

                AnimatedContent(
                    targetState = step,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    // The design's motion: new content rises 14px while fading
                    // in over 480ms; old content drifts 8px up and fades over
                    // 260ms — same direction both ways, like the prototype.
                    transitionSpec = {
                        val enter = fadeIn(tween(480, easing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f))) +
                            slideInVertically(tween(480, easing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f))) {
                                (14 * density).toInt()
                            }
                        val exit = fadeOut(tween(260, easing = CubicBezierEasing(0.42f, 0f, 1f, 1f))) +
                            slideOutVertically(tween(260, easing = CubicBezierEasing(0.42f, 0f, 1f, 1f))) {
                                (-8 * density).toInt()
                            }
                        enter togetherWith exit
                    },
                    label = "checkinStep"
                ) { current ->
                    when (current) {
                        CheckinStep.GREET -> CheckinGreetingStep(
                            onBegin = viewModel::goToNextStep
                        )

                        CheckinStep.ENERGY,
                        CheckinStep.SLEEP,
                        CheckinStep.STRESS,
                        CheckinStep.BODY,
                        CheckinStep.MOTIVATION -> {
                            val spec = questionSpecFor(current)
                            MoodQuestionStep(
                                spec = spec,
                                value = state.mood.valueFor(current),
                                comment = state.mood.commentFor(current),
                                noteOpen = current in openNotes,
                                onValueChange = { v -> viewModel.updateMoodValue(current, v) },
                                onCommentChange = { c -> viewModel.updateMoodComment(current, c) },
                                onToggleNote = {
                                    openNotes = if (current in openNotes) {
                                        openNotes - current
                                    } else {
                                        openNotes + current
                                    }
                                }
                            )
                        }

                        CheckinStep.RECAP -> MoodRecapStep(
                            rows = designedQuestionSteps.map { s ->
                                RecapRow(
                                    step = s,
                                    label = questionSpecFor(s).eyebrow,
                                    value = state.mood.valueFor(s),
                                    hasNote = state.mood.commentFor(s).isNotBlank()
                                )
                            },
                            onAmend = viewModel::goToQuestion
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
                            onDiscard = onDismiss,
                            onEditBlock = viewModel::editBlock,
                            onRemoveBlock = viewModel::removeBlock,
                            onAddBlock = viewModel::addBlock
                        )
                    }
                }

                AnimatedVisibility(visible = inFlow) {
                    CheckinFlowBottomBar(
                        label = when (step) {
                            CheckinStep.MOTIVATION -> "Finish"
                            CheckinStep.RECAP -> "Generate my evening"
                            else -> "Next"
                        },
                        alpha = chromeAlpha,
                        enabled = step != CheckinStep.GREET,
                        onClick = viewModel::goToNextStep
                    )
                }
            }
        }
    }
}

/** The mood value backing one question step. */
private fun CheckinMoodState.valueFor(step: CheckinStep): Int =
    when (step) {
        CheckinStep.ENERGY -> energy
        CheckinStep.SLEEP -> sleepiness
        CheckinStep.STRESS -> stress
        CheckinStep.BODY -> headache
        CheckinStep.MOTIVATION -> motivation
        else -> 0
    }

/** The optional note backing one question step. */
private fun CheckinMoodState.commentFor(step: CheckinStep): String =
    when (step) {
        CheckinStep.ENERGY -> energyComment
        CheckinStep.SLEEP -> sleepinessComment
        CheckinStep.STRESS -> stressComment
        CheckinStep.BODY -> headacheComment
        CheckinStep.MOTIVATION -> motivationComment
        else -> ""
    }
