package com.habit.application.todo

import com.habit.domain.model.TodoItem
import com.habit.domain.model.TodoPriority
import com.habit.domain.model.TodoStatus
import com.habit.domain.model.TodoSubtask
import com.habit.domain.repository.HabitRepository
import com.habit.domain.repository.TodoCategoryRepository
import com.habit.domain.repository.TodoItemRepository
import com.habit.domain.service.TodoHabitSyncDomainService
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class EnrichedTodoItem(
    val todo: TodoItem,
    val categoryName: String?,
    val categoryColorHex: String?,
    val linkedHabitTitle: String?,
    val effectiveLinkedHabitIds: List<UUID>,
    val effectiveLinkedHabitTitles: List<String>
)

open class ManageTodoItemUseCase(
    private val todoRepository: TodoItemRepository,
    private val categoryRepository: TodoCategoryRepository,
    private val habitRepository: HabitRepository,
    private val todoHabitSyncService: TodoHabitSyncDomainService
) {

    @Transactional(readOnly = true)
    open fun listTodos(
        userId: String,
        status: TodoStatus? = null,
        categoryId: UUID? = null,
        priority: TodoPriority? = null
    ): List<EnrichedTodoItem> {
        val todos = todoRepository.findByUserId(userId, status, categoryId, priority)
        if (todos.isEmpty()) return emptyList()

        val categoriesById = categoryRepository.findByUserId(userId).associateBy { it.id }
        val habitsById = habitRepository.findByUserId(userId, includeArchived = false).associateBy { it.id }

        return todos.map { todo ->
            enrichTodo(todo, categoriesById, habitsById)
        }
    }

    @Transactional
    open fun createTodo(
        userId: String,
        title: String,
        description: String?,
        categoryId: UUID?,
        linkedHabitId: UUID?,
        priority: TodoPriority?,
        dueDate: LocalDate?,
        subtaskTitles: List<String>?
    ): EnrichedTodoItem {
        require(title.isNotBlank()) { "O título da tarefa é obrigatório" }
        val todoId = UUID.randomUUID()
        val subtasks = subtaskTitles.orEmpty()
            .filter { it.isNotBlank() }
            .mapIndexed { idx, stTitle ->
                TodoSubtask(
                    id = UUID.randomUUID(),
                    todoId = todoId,
                    title = stTitle.trim(),
                    isCompleted = false,
                    sortOrder = idx,
                    createdAt = Instant.now()
                )
            }

        val todo = TodoItem(
            id = todoId,
            userId = userId,
            title = title.trim(),
            description = description?.trim(),
            categoryId = categoryId,
            linkedHabitId = linkedHabitId,
            priority = priority ?: TodoPriority.MEDIUM,
            status = TodoStatus.PENDING,
            dueDate = dueDate,
            subtasks = subtasks,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val saved = todoRepository.save(todo)
        return enrichSingle(saved, userId)
    }

    @Transactional
    open fun updateTodo(
        userId: String,
        todoId: UUID,
        title: String,
        description: String?,
        categoryId: UUID?,
        linkedHabitId: UUID?,
        priority: TodoPriority?,
        dueDate: LocalDate?
    ): EnrichedTodoItem {
        val existing = todoRepository.findById(todoId)
            ?: throw NoSuchElementException("Tarefa não encontrada: $todoId")

        val updated = existing.copy(
            title = title.trim(),
            description = description?.trim(),
            categoryId = categoryId,
            linkedHabitId = linkedHabitId,
            priority = priority ?: existing.priority,
            dueDate = dueDate,
            updatedAt = Instant.now()
        )
        val saved = todoRepository.save(updated)

        if (saved.status == TodoStatus.COMPLETED) {
            todoHabitSyncService.syncOnTodoStatusChanged(saved)
        }

        return enrichSingle(saved, userId)
    }

    @Transactional
    open fun deleteTodo(userId: String, todoId: UUID) {
        todoRepository.findById(todoId)
            ?: throw NoSuchElementException("Tarefa não encontrada: $todoId")

        todoHabitSyncService.reverseOnTodoDeleted(todoId)
        todoRepository.deleteById(todoId)
    }

    @Transactional
    open fun addSubtask(userId: String, todoId: UUID, title: String): EnrichedTodoItem {
        require(title.isNotBlank()) { "O título da subtarefa é obrigatório" }
        val existing = todoRepository.findById(todoId)
            ?: throw NoSuchElementException("Tarefa não encontrada: $todoId")

        val newSubtask = TodoSubtask(
            id = UUID.randomUUID(),
            todoId = existing.id,
            title = title.trim(),
            isCompleted = false,
            sortOrder = existing.subtasks.size,
            createdAt = Instant.now()
        )
        val saved = todoRepository.save(
            existing.copy(
                subtasks = existing.subtasks + newSubtask,
                updatedAt = Instant.now()
            )
        )
        return enrichSingle(saved, userId)
    }

    @Transactional
    open fun toggleSubtask(userId: String, todoId: UUID, subtaskId: UUID): EnrichedTodoItem {
        val existing = todoRepository.findById(todoId)
            ?: throw NoSuchElementException("Tarefa não encontrada: $todoId")

        val updatedSubtasks = existing.subtasks.map { st ->
            if (st.id == subtaskId) {
                val nextCompleted = !st.isCompleted
                st.copy(
                    isCompleted = nextCompleted,
                    completedAt = if (nextCompleted) Instant.now() else null
                )
            } else {
                st
            }
        }

        // Nota: Marcar/desmarcar subtarefa NÃO altera hábito vinculado (Decisão D-08)
        val saved = todoRepository.save(
            existing.copy(
                subtasks = updatedSubtasks,
                updatedAt = Instant.now()
            )
        )
        return enrichSingle(saved, userId)
    }

    @Transactional
    open fun deleteSubtask(userId: String, todoId: UUID, subtaskId: UUID): EnrichedTodoItem {
        val existing = todoRepository.findById(todoId)
            ?: throw NoSuchElementException("Tarefa não encontrada: $todoId")

        val saved = todoRepository.save(
            existing.copy(
                subtasks = existing.subtasks.filterNot { it.id == subtaskId },
                updatedAt = Instant.now()
            )
        )
        return enrichSingle(saved, userId)
    }

    open fun enrichSingle(todo: TodoItem, userId: String): EnrichedTodoItem {
        val categoriesById = categoryRepository.findByUserId(userId).associateBy { it.id }
        val habitsById = habitRepository.findByUserId(userId, includeArchived = false).associateBy { it.id }
        return enrichTodo(todo, categoriesById, habitsById)
    }

    private fun enrichTodo(
        todo: TodoItem,
        categoriesById: Map<UUID, com.habit.domain.model.TodoCategory>,
        habitsById: Map<UUID, com.habit.domain.model.Habit>
    ): EnrichedTodoItem {
        val category = todo.categoryId?.let { categoriesById[it] }
        val directHabit = todo.linkedHabitId?.let { habitsById[it] }
        val targetHabits = todoHabitSyncService.resolveTargetHabitsForTodo(todo)

        return EnrichedTodoItem(
            todo = todo,
            categoryName = category?.name,
            categoryColorHex = category?.colorHex,
            linkedHabitTitle = directHabit?.title,
            effectiveLinkedHabitIds = targetHabits.map { it.id },
            effectiveLinkedHabitTitles = targetHabits.map { it.title }
        )
    }
}
