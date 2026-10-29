package com.habit.domain.repository

import com.habit.domain.model.TodoCategory
import java.util.UUID

interface TodoCategoryRepository {
    fun save(category: TodoCategory): TodoCategory
    fun findById(id: UUID): TodoCategory?
    fun findByUserId(userId: String): List<TodoCategory>
    fun deleteById(id: UUID)
}
