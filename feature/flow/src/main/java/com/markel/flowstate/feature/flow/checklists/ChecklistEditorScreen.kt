package com.markel.flowstate.feature.flow.checklists

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markel.flowstate.core.designsystem.ui.CheckListSharedKeys
import com.markel.flowstate.core.designsystem.ui.sharedDetailBounds
import com.markel.flowstate.feature.flow.checklists.components.CheckListItemRow
import com.markel.flowstate.feature.flow.checklists.components.GhostItemRow
import com.markel.flowstate.feature.flow.components.COLOR_TRANSPARENT
import com.markel.flowstate.feature.flow.components.ColorPicker
import com.markel.flowstate.feature.flow.components.resolveIdeaColor
import com.markel.flowstate.feature.tasks.R

/**
 * Full screen for checklist editing/creation.
 *
 * [checkListId] = null → new checklist mode
 * [checkListId] = Int → existing checklist editing mode
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckListEditorScreen(
    checkListId: Int?,
    onBack: () -> Unit,
    viewModel: CheckListViewModel = hiltViewModel()
) {
    BackHandler {
        viewModel.closeAndSave()
        onBack()
    }

    val editorState by viewModel.editor.collectAsStateWithLifecycle()

    LaunchedEffect(checkListId) {
        if (checkListId == null) viewModel.openNew()
        else viewModel.loadForEditing(checkListId)
    }

    val resolvedColor = editorState.color.resolveIdeaColor()
    val cardColor = if (resolvedColor == COLOR_TRANSPARENT)
        MaterialTheme.colorScheme.surface
    else
        Color(resolvedColor)

    val onCardColor = MaterialTheme.colorScheme.onSurface
    var showColorSheet by remember { mutableStateOf(false) }
    // Tracks which item id should steal focus (set after addItem())
    var pendingFocusId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        modifier = Modifier.sharedDetailBounds(
            key = checkListId?.let { CheckListSharedKeys.container(it) }
                ?: "checklist_new"
        ),
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
                    if (checkListId != null) {
                        IconButton(onClick = {
                            viewModel.deleteCheckList(checkListId)
                            onBack()
                        }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.delete_24px),
                                contentDescription = "Delete checklist",
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
            // Title field
            BasicTextField(
                value = editorState.title,
                onValueChange = { viewModel.updateTitle(it) },
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

            Spacer(Modifier.height(20.dp))

            // Checklist items
            editorState.items.forEachIndexed { index, item ->
                val requestFocus = pendingFocusId == item.id
                CheckListItemRow(
                    text = item.text,
                    isDone = item.isDone,
                    requestFocusOnAppear = requestFocus,
                    onFocusConsumed = { if (requestFocus) pendingFocusId = null },
                    onTextChange = { viewModel.updateItemText(item.id, it) },
                    onToggle = { viewModel.toggleItem(item.id) },
                    onDelete = { viewModel.removeItem(item.id) },
                    onAddNext = {
                        val newId = viewModel.addItem()
                        pendingFocusId = newId
                    },
                    onCardColor = onCardColor
                )
            }
            // Ghost row — always visible at the bottom, tapping it creates a real item
            GhostItemRow(
                onCardColor = onCardColor,
                onClick = {
                    val newId = viewModel.addItem()
                    pendingFocusId = newId
                }
            )
            Spacer(Modifier.height(40.dp))
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