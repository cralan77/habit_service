package com.habit.application.todo

import com.habit.domain.model.TodoCategory
import com.habit.domain.repository.TodoCategoryRepository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

open class ManageTodoCategoryUseCase(
    private val categoryRepository: TodoCategoryRepository
) {

    @Transactional(readOnly = true)
    open fun listCategories(userId: String): List<TodoCategory> {
        return categoryRepository.findByUserId(userId)
    }

    @Transactional
    open fun createCategory(
        userId: String,
        name: String,
        colorHex: String?,
        icon: String?
    ): TodoCategory {
        require(name.isNotBlank()) { "O nome da categoria é obrigatório" }
        val category = TodoCategory(
            id = UUID.randomUUID(),
            userId = userId,
            name = name.trim(),
            colorHex = colorHex?.takeIf { it.isNotBlank() } ?: "#6366F1",
            icon = icon?.takeIf { it.isNotBlank() } ?: "folder",
            isArchived = false,
            createdAt = Instant.now()
        )
        return categoryRepository.save(category)
    }

    @Transactional
    open fun updateCategory(
        userId: String,
        categoryId: UUID,
        name: String,
        colorHex: String?,
        icon: String?,
        isArchived: Boolean?
    ): TodoCategory {
        val existing = categoryRepository.findById(categoryId)
            ?: throw NoSuchElementException("Categoria não encontrada: $categoryId")

        val updated = existing.copy(
            name = name.trim(),
            colorHex = colorHex?.takeIf { it.isNotBlank() } ?: existing.colorHex,
            icon = icon?.takeIf { it.isNotBlank() } ?: existing.icon,
            isArchived = isArchived ?: existing.isArchived
        )
        return categoryRepository.save(updated)
    }

    @Transactional
    open fun deleteCategory(userId: String, categoryId: UUID) {
        categoryRepository.findById(categoryId)
            ?: throw NoSuchElementException("Categoria não encontrada: $categoryId")
        categoryRepository.deleteById(categoryId)
    }
}
