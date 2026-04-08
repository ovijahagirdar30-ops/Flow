package com.markel.flowstate.core.domain

data class Idea(
    val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val color: Long,
    val position: Int = 0
)