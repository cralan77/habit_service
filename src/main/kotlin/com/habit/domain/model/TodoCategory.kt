package com.habit.domain.model

import java.time.Instant
import java.util.UUID

data class TodoCategory(
    val id: UUID = UUID.randomUUID(),
    val userId: String,
    val name: String,
    val colorHex: String = "#6366F1",
    val icon: String = "folder",
    val isArchived: Boolean = false,
    val createdAt: Instant = Instant.now()
)
