package com.markel.flowstate.feature.flow.ideas

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markel.flowstate.feature.flow.ideas.components.IDEA_COLOR_TRANSPARENT
import com.markel.flowstate.feature.flow.ideas.components.IdeaColorPicker
import com.markel.flowstate.feature.flow.ideas.components.resolveIdeaColor
import com.markel.flowstate.feature.tasks.R

/**
 * Full-screen overlay for creating / editing an idea.
 * Auto-saves on back / close via [onClose]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaEditorOverlay(
    editorState: IdeaEditorState,
    onClose: () -> Unit,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onColorChange: (Long) -> Unit
) {

    BackHandler { onClose() }

    val resolvedColor = editorState.color.resolveIdeaColor()
    val cardColor = if (resolvedColor == IDEA_COLOR_TRANSPARENT)
        MaterialTheme.colorScheme.surface
    else
        Color(resolvedColor)

    // For text legibility use a dark tint over light backgrounds
    val onCardColor = MaterialTheme.colorScheme.onSurface

    val contentFocusRequester = remember { FocusRequester() }
    var showColorSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Auto-focus content area when opening a new idea
        if (editorState.idea == null) contentFocusRequester.requestFocus()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = cardColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cardColor,
                    navigationIconContentColor = onCardColor,
                    actionIconContentColor = onCardColor
                ),
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Close editor"
                        )
                    }
                },
                title = {},
                actions = {
                    IconButton(onClick = { showColorSheet = true }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.palette_24px),
                            contentDescription = "Cambiar color de fondo",
                            tint = onCardColor.copy(alpha = 0.8f)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // ── Title ─────────────────────────────────────────────────────────
            BasicTextField(
                value = editorState.title,
                onValueChange = onTitleChange,
                textStyle = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onCardColor
                ),
                cursorBrush = SolidColor(onCardColor),
                decorationBox = { inner ->
                    Box {
                        if (editorState.title.isEmpty()) {
                            Text(
                                text = "Title",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = onCardColor.copy(alpha = 0.4f)
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // ── Content ───────────────────────────────────────────────────────
            BasicTextField(
                value = editorState.content,
                onValueChange = onContentChange,
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = onCardColor.copy(alpha = 0.85f)
                ),
                cursorBrush = SolidColor(onCardColor),
                decorationBox = { inner ->
                    Box {
                        if (editorState.content.isEmpty()) {
                            Text(
                                text = "Start writing...",
                                fontSize = 16.sp,
                                color = onCardColor.copy(alpha = 0.35f)
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 300.dp)
                    .focusRequester(contentFocusRequester)
            )
        }
    }

    // ── Color picker bottom sheet ─────────────────────────────────────────────
    if (showColorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showColorSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = null,
            containerColor = cardColor, // Sheet background matches the selected card color for immersion
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.24f),
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Background color",
                    style = MaterialTheme.typography.titleSmall,
                    color = onCardColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Spacer(Modifier.height(8.dp))
                IdeaColorPicker(
                    selectedColor = editorState.color,
                    onColorSelected = {
                        onColorChange(it)
                    }
                )
            }
        }
    }

}