package com.markel.flowstate.feature.flow.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markel.flowstate.core.domain.Priority
import com.markel.flowstate.core.domain.SubTask
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.feature.tasks.R
import kotlinx.coroutines.delay
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheetContent(
    task: Task?,
    priority: Priority, // Get it from the parent
    dueDate: Long?,
    onAutoUpdate: (String, String, Priority, Long?, List<SubTask>) -> Unit
) {
    val isNewTask = remember { task == null }
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    val subTasks = remember {
        mutableStateListOf<SubTask>().apply {
            addAll(task?.subTasks ?: emptyList())
        }
    }

    // Track which subtask is expanded for inline editing
    var expandedSubTaskId by remember { mutableStateOf<String?>(null) }

    // States to show the creation sheet when creating a subtask
    var showCreationSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Draft states for the new subtask
    var draftSubTitle by rememberSaveable { mutableStateOf("") }
    var draftSubDescription by rememberSaveable { mutableStateOf("") }
    var draftSubPriority by rememberSaveable { mutableStateOf(Priority.NOTHING) }
    var draftSubDueDate by rememberSaveable { mutableStateOf<Long?>(null) }

    // Checkpoints to track what was actually last saved
    var lastSavedTitle by remember { mutableStateOf(title) }
    var lastSavedDesc by remember { mutableStateOf(description) }
    var lastSavedPriority by remember { mutableStateOf(priority) }
    var lastSavedDueDate by remember { mutableStateOf(dueDate) }
    var lastSavedSubTasksHash by remember { mutableIntStateOf(subTasks.toList().hashCode()) }

    val focusRequester = remember { FocusRequester() }

    val currentPriority by rememberUpdatedState(priority)
    val currentDueDate by rememberUpdatedState(dueDate)
    val currentTitle by rememberUpdatedState(title)
    val currentDescription by rememberUpdatedState(description)
    val currentSubTasksList by rememberUpdatedState(subTasks.toList())

    // TIME-BASED AUTOSAVE (DEBOUNCE)
    if (!isNewTask) {
        LaunchedEffect(title, description, priority, dueDate, subTasks.toList().hashCode()) {
            val currentSubTasksHash = subTasks.toList().hashCode()

            val hasChanges = title != lastSavedTitle ||
                    description != lastSavedDesc ||
                    priority != lastSavedPriority ||
                    dueDate != lastSavedDueDate ||
                    currentSubTasksHash != lastSavedSubTasksHash

            if (hasChanges && title.isNotBlank()) {
                delay(600)
                // Save
                onAutoUpdate(title, description, priority, dueDate, subTasks.toList())
                // Update references
                lastSavedTitle = title
                lastSavedDesc = description
                lastSavedPriority = priority
                lastSavedDueDate = dueDate
                lastSavedSubTasksHash = currentSubTasksHash
            }
        }
    }

    // EMERGENCY SAVE ON CLOSE (DISPOSE)
    // This runs if the user closes the sheet before the delay finishes
    DisposableEffect(Unit) {
        onDispose {
            // Only autosave on exit if it's an ALREADY EXISTING task (Editing)
            if (!isNewTask) {
                val currentSubTasksHash = currentSubTasksList.hashCode()
                val hasPendingChanges = currentTitle != lastSavedTitle ||
                        currentDescription != lastSavedDesc ||
                        currentPriority != lastSavedPriority ||
                        currentDueDate != lastSavedDueDate ||
                        currentSubTasksHash != lastSavedSubTasksHash

                if (hasPendingChanges && currentTitle.isNotBlank()) {
                    onAutoUpdate(
                        currentTitle,
                        currentDescription,
                        currentPriority,
                        currentDueDate,
                        currentSubTasksList
                    )
                }
            }
        }
    }

    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {

        // TASK
        TextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = {
                Text(
                    stringResource(R.string.edit_task_placeholder),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            )
        )

        TextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    stringResource(R.string.edit_task_desc_placeholder),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // SUBTASKS
        val visibleSubTasks = remember(subTasks.toList()) {
            subTasks.filter { !it.isDone }
        }

        Text(
            stringResource(R.string.subtasks),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.height(8.dp))

        // list of subtasks
        if (visibleSubTasks.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    visibleSubTasks.forEachIndexed { index, subTask ->
                        EditableSubTaskItem(
                            subTask = subTask,
                            isExpanded = expandedSubTaskId == subTask.id,
                            onExpandChange = { shouldExpand ->
                                expandedSubTaskId = if (shouldExpand) subTask.id else null
                            },
                            onUpdate = { updatedSubTask ->
                                val realIndex = subTasks.indexOfFirst { it.id == updatedSubTask.id }
                                if (realIndex != -1) {
                                    subTasks[realIndex] = updatedSubTask
                                }
                            },
                            onCheckedChange = {
                                // We need to find the index in the REAL list, not the visible list
                                val realIndex = subTasks.indexOfFirst { it.id == subTask.id }
                                if (realIndex != -1) {
                                    subTasks[realIndex] = subTask.copy(isDone = !subTask.isDone)
                                }
                            },
                            onDelete = {
                                val realIndex = subTasks.indexOfFirst { it.id == subTask.id }
                                if (realIndex != -1) {
                                    subTasks.removeAt(realIndex)
                                }
                                // Close expansion if this was the expanded item
                                if (expandedSubTaskId == subTask.id) {
                                    expandedSubTaskId = null
                                }
                            }
                        )
                        if (index < visibleSubTasks.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Button to create a new subtask
        TextButton(
            onClick = {
                // Close any expanded subtask before opening creation sheet
                expandedSubTaskId = null
                showCreationSheet = true
            },
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.tertiary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_subtask), fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showCreationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreationSheet = false },
            sheetState = sheetState,
            dragHandle = null,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            TaskCreationSheetContent(
                title = draftSubTitle,
                onTitleChange = { draftSubTitle = it },
                description = draftSubDescription,
                onDescriptionChange = { draftSubDescription = it },
                priority = draftSubPriority,
                onPriorityChange = { draftSubPriority = it },
                dueDate = draftSubDueDate,
                onDueDateChange = { draftSubDueDate = it },
                titlePlaceholder = stringResource(R.string.add_subtask_placeholder),
                onSave = { title, desc, prio, date ->
                    val newSubTask = SubTask(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        description = desc,
                        isDone = false,
                        priority = prio,
                        dueDate = date,
                        position = subTasks.size
                    )
                    subTasks.add(newSubTask)

                    draftSubTitle = ""
                    draftSubDescription = ""
                    draftSubPriority = Priority.NOTHING
                    draftSubDueDate = null
                    showCreationSheet = false
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        if (isNewTask) focusRequester.requestFocus()
    }
}