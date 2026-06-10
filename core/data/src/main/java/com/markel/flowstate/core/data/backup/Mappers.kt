package com.markel.flowstate.core.data.backup

import com.markel.flowstate.core.data.local.*

// ── Task ──────────────────────────────────────────────────────────────

fun TaskEntity.toSchema() = TaskSchema(
    id = id,
    title = title,
    description = description,
    isDone = isDone,
    position = position,
    priority = priority,
    dueDate = dueDate,
    completedAt = completedAt,
    reminderTime = reminderTime
)

fun TaskSchema.toEntity() = TaskEntity(
    id = id,
    title = title,
    description = description,
    isDone = isDone,
    position = position,
    priority = priority,
    dueDate = dueDate,
    completedAt = completedAt,
    reminderTime = reminderTime
)

// ── SubTask ───────────────────────────────────────────────────────────

fun SubTaskEntity.toSchema() = SubTaskSchema(
    id = id,
    taskId = taskId,
    title = title,
    description = description,
    isDone = isDone,
    priority = priority,
    dueDate = dueDate,
    position = position,
    completedAt = completedAt,
    reminderTime = reminderTime
)

fun SubTaskSchema.toEntity() = SubTaskEntity(
    id = id,
    taskId = taskId,
    title = title,
    description = description,
    isDone = isDone,
    priority = priority,
    dueDate = dueDate,
    position = position,
    completedAt = completedAt,
    reminderTime = reminderTime
)

// ── Idea ──────────────────────────────────────────────────────────────

fun IdeaEntity.toSchema() = IdeaSchema(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    color = color,
    position = position
)

fun IdeaSchema.toEntity() = IdeaEntity(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    color = color,
    position = position
)

// ── Checklist ─────────────────────────────────────────────────────────

fun CheckListEntity.toSchema() = CheckListSchema(
    id = id,
    title = title,
    color = color,
    position = position
)

fun CheckListSchema.toEntity() = CheckListEntity(
    id = id,
    title = title,
    color = color,
    position = position
)

fun CheckListItemEntity.toSchema() = CheckListItemSchema(
    id = id,
    listId = listId,
    text = text,
    isDone = isDone,
    position = position
)

fun CheckListItemSchema.toEntity() = CheckListItemEntity(
    id = id,
    listId = listId,
    text = text,
    isDone = isDone,
    position = position
)

// ── Habit ─────────────────────────────────────────────────────────────

fun HabitEntity.toSchema() = HabitSchema(
    id = id,
    name = name,
    iconName = iconName,
    colorArgb = colorArgb,
    frequency = frequency,
    createdAt = createdAt,
    habitType = habitType,
    unit = unit,
    targetValue = targetValue,
    step = step,
    position = position
)

fun HabitSchema.toEntity() = HabitEntity(
    id = id,
    name = name,
    iconName = iconName,
    colorArgb = colorArgb,
    frequency = frequency,
    createdAt = createdAt,
    habitType = habitType,
    unit = unit,
    targetValue = targetValue,
    step = step,
    position = position
)

fun HabitEntryEntity.toSchema() = HabitEntrySchema(
    id = id,
    habitId = habitId,
    completedAt = completedAt
)

fun HabitEntrySchema.toEntity() = HabitEntryEntity(
    id = id,
    habitId = habitId,
    completedAt = completedAt
)

fun HabitNumericEntryEntity.toSchema() = HabitNumericEntrySchema(
    habitId = habitId,
    epochDay = epochDay,
    value = value
)

fun HabitNumericEntrySchema.toEntity() = HabitNumericEntryEntity(
    habitId = habitId,
    epochDay = epochDay,
    value = value
)
