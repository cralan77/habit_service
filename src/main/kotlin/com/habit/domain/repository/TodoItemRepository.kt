package com.habit.domain.repository

import com.habit.domain.model.TodoItem
import com.habit.domain.model.TodoPriority
import com.habit.domain.model.TodoStatus
import java.util.UUID

interface TodoItemRepository {
    fun save(todo: TodoItem): TodoItem
    fun findById(id: UUID): TodoItem?
    fun findByUserId(
        userId: String,
        status: TodoStatus? = null,
        categoryId: UUID? = null,
        priority: TodoPriority? = null
    ): List<TodoItem>
    fun deleteById(id: UUID)
}
