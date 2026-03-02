package com.markel.flowstate.core.domain

import java.util.UUID

data class CheckList(
    val id: Int = 0,
    val title: String,
    val color: Long,
    val items: List<CheckListItem> = emptyList()
)

data class CheckListItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false,
    val position: Int = 0
)