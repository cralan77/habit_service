package com.habit.domain.service

import com.habit.domain.model.CompletionMode
import com.habit.domain.model.Habit
import com.habit.domain.model.HabitExecutionLog
import com.habit.domain.model.HabitLogSource
import com.habit.domain.model.HabitLogStatus
import com.habit.domain.model.TodoItem
import com.habit.domain.model.TodoStatus
import com.habit.domain.repository.HabitExecutionLogRepository
import com.habit.domain.repository.HabitRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class TodoHabitSyncDomainService(
    private val habitRepository: HabitRepository,
    private val logRepository: HabitExecutionLogRepository
) {

    /**
     * Resolve sem duplicidade (Set<UUID>) todos os hábitos vinculados a uma Tarefa Principal (TodoItem):
     * 1. Hábito vinculado diretamente via [todo.linkedHabitId]
     * 2. Hábitos ativos do usuário com completionMode == TODO_LINKED e linkedTodoCategoryId == [todo.categoryId]
     */
    fun resolveTargetHabitsForTodo(todo: TodoItem): List<Habit> {
        val targetHabitsById = linkedMapOf<UUID, Habit>()

        todo.linkedHabitId?.let { directHabitId ->
            val directHabit = habitRepository.findById(directHabitId)
            if (directHabit != null && directHabit.isActive) {
                targetHabitsById[directHabit.id] = directHabit
            }
        }

        todo.categoryId?.let { categoryId ->
            val categoryHabits = habitRepository.findByLinkedTodoCategoryId(todo.userId, categoryId)
            categoryHabits
                .filter { it.isActive && it.completionMode == CompletionMode.TODO_LINKED }
                .forEach { habit ->
                    targetHabitsById[habit.id] = habit
                }
        }

        return targetHabitsById.values.toList()
    }

    /**
     * Quando uma Tarefa Principal (TodoItem) muda de status:
     * - Se COMPLETED: gera um registro HabitExecutionLog (+1) para cada hábito vinculado (sem duplicidade).
     * - Se PENDING (reaberta): estorna automaticamente removendo os logs gerados por esta tarefa.
     * Subtarefas (TodoSubtask) NÃO chamam este método.
     */
    fun syncOnTodoStatusChanged(
        todo: TodoItem,
        executionDate: LocalDate = todo.completedAt?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: LocalDate.now()
    ): List<Habit> {
        val referenceId = todo.id.toString()

        if (todo.status == TodoStatus.PENDING) {
            logRepository.deleteBySourceAndReferenceId(HabitLogSource.TODO_LINKED, referenceId)
            return resolveTargetHabitsForTodo(todo)
        }

        // Primeiro limpa eventuais logs órfãos caso a tarefa tenha mudado de categoria/hábito antes de concluir
        logRepository.deleteBySourceAndReferenceId(HabitLogSource.TODO_LINKED, referenceId)

        val targetHabits = resolveTargetHabitsForTodo(todo)
        targetHabits.forEach { habit ->
            val existing = logRepository.findByHabitIdAndSourceAndReferenceId(
                habitId = habit.id,
                source = HabitLogSource.TODO_LINKED,
                sourceReferenceId = referenceId
            )
            if (existing == null) {
                logRepository.save(
                    HabitExecutionLog(
                        id = UUID.randomUUID(),
                        habitId = habit.id,
                        userId = habit.userId,
                        executionDate = executionDate,
                        completedValue = 1.0,
                        status = HabitLogStatus.COMPLETED,
                        source = HabitLogSource.TODO_LINKED,
                        sourceReferenceId = referenceId,
                        notes = "Concluído via To-Do: ${todo.title}",
                        createdAt = Instant.now(),
                        updatedAt = Instant.now()
                    )
                )
            }
        }

        return targetHabits
    }

    /**
     * Estorna qualquer progresso de hábito caso a tarefa seja excluída.
     */
    fun reverseOnTodoDeleted(todoId: UUID) {
        logRepository.deleteBySourceAndReferenceId(HabitLogSource.TODO_LINKED, todoId.toString())
    }
}
