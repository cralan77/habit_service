package com.habit.interfaces.rest.dto

import com.habit.application.todo.EnrichedTodoItem
import com.habit.domain.model.TodoCategory
import com.habit.domain.model.TodoPriority
import com.habit.domain.model.TodoStatus
import com.habit.domain.model.TodoSubtask
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreateOrUpdateTodoCategoryRequest(
    val name: String,
    val colorHex: String? = "#6366F1",
    val icon: String? = "folder",
    val isArchived: Boolean? = false
)

data class TodoCategoryResponse(
    val id: UUID,
    val userId: String,
    val name: String,
    val colorHex: String,
    val icon: String,
    val isArchived: Boolean,
    val createdAt: Instant
) {
    companion object {
        fun from(domain: TodoCategory) = TodoCategoryResponse(
            id = domain.id,
            userId = domain.userId,
            name = domain.name,
            colorHex = domain.colorHex,
            icon = domain.icon,
            isArchived = domain.isArchived,
            createdAt = domain.createdAt
        )
    }
}

data class CreateTodoRequest(
    val title: String,
    val description: String? = null,
    val categoryId: UUID? = null,
    val linkedHabitId: UUID? = null,
    val priority: TodoPriority? = TodoPriority.MEDIUM,
    val dueDate: LocalDate? = null,
    val subtasks: List<String>? = null
)

data class UpdateTodoRequest(
    val title: String,
    val description: String? = null,
    val categoryId: UUID? = null,
    val linkedHabitId: UUID? = null,
    val priority: TodoPriority? = null,
    val dueDate: LocalDate? = null
)

data class CreateSubtaskRequest(
    val title: String
)

data class TodoSubtaskResponse(
    val id: UUID,
    val todoId: UUID,
    val title: String,
    val isCompleted: Boolean,
    val sortOrder: Int,
    val completedAt: Instant?
) {
    companion object {
        fun from(domain: TodoSubtask) = TodoSubtaskResponse(
            id = domain.id,
            todoId = domain.todoId,
            title = domain.title,
            isCompleted = domain.isCompleted,
            sortOrder = domain.sortOrder,
            completedAt = domain.completedAt
        )
    }
}

data class TodoItemResponse(
    val id: UUID,
    val userId: String,
    val title: String,
    val description: String?,
    val categoryId: UUID?,
    val categoryName: String?,
    val categoryColorHex: String?,
    val linkedHabitId: UUID?,
    val linkedHabitTitle: String?,
    val effectiveLinkedHabitIds: List<UUID>,
    val effectiveLinkedHabitTitles: List<String>,
    val priority: TodoPriority,
    val status: TodoStatus,
    val dueDate: LocalDate?,
    val completedAt: Instant?,
    val subtasks: List<TodoSubtaskResponse>,
    val subtasksCompleted: Int,
    val subtasksTotal: Int,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(enriched: EnrichedTodoItem) = TodoItemResponse(
            id = enriched.todo.id,
            userId = enriched.todo.userId,
            title = enriched.todo.title,
            description = enriched.todo.description,
            categoryId = enriched.todo.categoryId,
            categoryName = enriched.categoryName,
            categoryColorHex = enriched.categoryColorHex,
            linkedHabitId = enriched.todo.linkedHabitId,
            linkedHabitTitle = enriched.linkedHabitTitle,
            effectiveLinkedHabitIds = enriched.effectiveLinkedHabitIds,
            effectiveLinkedHabitTitles = enriched.effectiveLinkedHabitTitles,
            priority = enriched.todo.priority,
            status = enriched.todo.status,
            dueDate = enriched.todo.dueDate,
            completedAt = enriched.todo.completedAt,
            subtasks = enriched.todo.subtasks.map { TodoSubtaskResponse.from(it) },
            subtasksCompleted = enriched.todo.subtasks.count { it.isCompleted },
            subtasksTotal = enriched.todo.subtasks.size,
            createdAt = enriched.todo.createdAt,
            updatedAt = enriched.todo.updatedAt
        )
    }
}
