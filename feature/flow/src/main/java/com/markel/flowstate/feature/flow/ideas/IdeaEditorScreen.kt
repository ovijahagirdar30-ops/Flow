package com.markel.flowstate.feature.flow.ideas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markel.flowstate.core.designsystem.ui.IdeaSharedKeys
import com.markel.flowstate.core.designsystem.ui.sharedDetailBounds
import com.markel.flowstate.feature.flow.components.COLOR_TRANSPARENT
import com.markel.flowstate.feature.flow.components.ColorPicker
import com.markel.flowstate.feature.flow.components.resolveIdeaColor
import com.markel.flowstate.feature.tasks.R

/**
 * Full screen for idea editing/creation.
 *
 * As a standalone navigation destination:
 * - The bottom bar automatically disappears (controlled by route in MainActivity)
 * - The Navigation back stack manages "back"
 *
 * [ideaId] = null → new idea creation mode
 * [ideaId] = Int → existing idea editing mode
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaEditorScreen(
    ideaId: Int?, // null = new idea
    onBack: () -> Unit,
    viewModel: IdeaEditorViewModel = hiltViewModel()
) {
    BackHandler {
        viewModel.closeAndSave()
        onBack()
    }

    val editorState by viewModel.editor.collectAsStateWithLifecycle()

    // We initialize the editor's state depending on whether we are creating or editing
    LaunchedEffect(ideaId) {
        if (ideaId == null) viewModel.openNew()
        else viewModel.loadIdeaForEditing(ideaId)
    }
    
    val resolvedColor = editorState.color.resolveIdeaColor()
    val cardColor = if (resolvedColor == COLOR_TRANSPARENT)
        MaterialTheme.colorScheme.surface
    else
        Color(resolvedColor)

    val onCardColor = MaterialTheme.colorScheme.onSurface

    val contentFocusRequester = remember { FocusRequester() }
    var showColorSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .then(
                if (ideaId != null) Modifier.sharedDetailBounds(IdeaSharedKeys.container(ideaId))
                        else Modifier
            ),
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
                    IconButton(onClick = {
                        viewModel.closeAndSave()
                        onBack()
                    }) {
                        Icon(ImageVector.vectorResource(R.drawable.arrow_back_24px), contentDescription = "Back")
                    }
                },
                title = {},
                actions = {
                    IconButton(onClick = { showColorSheet = true }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.palette_24px),
                            contentDescription = "Change background color",
                            tint = onCardColor.copy(alpha = 0.8f)
                        )
                    }
                    if (ideaId != null) {
                        IconButton(onClick = {
                            viewModel.deleteIdea(ideaId)
                            onBack()
                        }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.delete_24px),
                                contentDescription = "Delete idea",
                                tint = onCardColor.copy(alpha = 0.8f)
                            )
                        }
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
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            BasicTextField(
                value = editorState.title,
                onValueChange = { viewModel.updateTitle(it) },
                textStyle = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onCardColor
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                cursorBrush = SolidColor(onCardColor),
                decorationBox = { inner ->
                    Box {
                        if (editorState.title.isEmpty()) {
                            Text(
                                text = stringResource(R.string.title),
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

            BasicTextField(
                value = editorState.content,
                onValueChange = { viewModel.updateContent(it) },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = onCardColor.copy(alpha = 0.85f)
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                cursorBrush = SolidColor(onCardColor),
                decorationBox = { inner ->
                    Box {
                        if (editorState.content.isEmpty()) {
                            Text(
                                text = stringResource(R.string.idea_content),
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

    if (showColorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showColorSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = null,
            containerColor = cardColor,
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.24f),
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.backg_color),
                    style = MaterialTheme.typography.titleSmall,
                    color = onCardColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Spacer(Modifier.height(8.dp))
                ColorPicker(
                    selectedColor = editorState.color,
                    onColorSelected = { viewModel.updateColor(it) }
                )
            }
        }
    }
}