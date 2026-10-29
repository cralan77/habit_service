package com.habit.application.todo

import com.habit.domain.model.TodoStatus
import com.habit.domain.repository.TodoItemRepository
import com.habit.domain.service.TodoHabitSyncDomainService
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

open class CompleteTodoItemUseCase(
    private val todoRepository: TodoItemRepository,
    private val todoHabitSyncService: TodoHabitSyncDomainService,
    private val manageTodoItemUseCase: ManageTodoItemUseCase
) {

    @Transactional
    open fun toggleTodoStatus(
        userId: String,
        todoId: UUID,
        executionDate: LocalDate = LocalDate.now()
    ): EnrichedTodoItem {
        val existing = todoRepository.findById(todoId)
            ?: throw NoSuchElementException("Tarefa não encontrada: $todoId")

        val nextStatus = if (existing.status == TodoStatus.COMPLETED) {
            TodoStatus.PENDING
        } else {
            TodoStatus.COMPLETED
        }

        val updated = existing.copy(
            status = nextStatus,
            completedAt = if (nextStatus == TodoStatus.COMPLETED) Instant.now() else null,
            updatedAt = Instant.now()
        )

        val saved = todoRepository.save(updated)
        todoHabitSyncService.syncOnTodoStatusChanged(saved, executionDate)

        return manageTodoItemUseCase.enrichSingle(saved, userId)
    }
}
