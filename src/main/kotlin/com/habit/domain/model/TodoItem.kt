package com.habit.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class TodoSubtask(
    val id: UUID = UUID.randomUUID(),
    val todoId: UUID,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val completedAt: Instant? = null,
    val createdAt: Instant = Instant.now()
)

data class TodoItem(
    val id: UUID = UUID.randomUUID(),
    val userId: String,
    val title: String,
    val description: String? = null,
    val categoryId: UUID? = null,
    val linkedHabitId: UUID? = null,
    val priority: TodoPriority = TodoPriority.MEDIUM,
    val status: TodoStatus = TodoStatus.PENDING,
    val dueDate: LocalDate? = null,
    val completedAt: Instant? = null,
    val subtasks: List<TodoSubtask> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
