package com.habit.infrastructure.persistence

import com.habit.domain.model.Habit
import com.habit.domain.model.HabitExecutionLog
import com.habit.domain.model.HabitLogSource
import com.habit.domain.model.HabitPeriodSkip
import com.habit.domain.model.TodoCategory
import com.habit.domain.model.TodoItem
import com.habit.domain.model.TodoPriority
import com.habit.domain.model.TodoStatus
import com.habit.domain.model.UserRoutineSettings
import com.habit.domain.repository.HabitExecutionLogRepository
import com.habit.domain.repository.HabitPeriodSkipRepository
import com.habit.domain.repository.HabitRepository
import com.habit.domain.repository.TodoCategoryRepository
import com.habit.domain.repository.TodoItemRepository
import com.habit.domain.repository.UserRoutineSettingsRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Component
class HabitRepositoryImpl(
    private val jpaRepository: SpringDataHabitRepository,
    private val userIdentifierResolver: UserIdentifierResolver
) : HabitRepository {

    override fun save(habit: Habit): Habit {
        val canonicalUserId = userIdentifierResolver.resolveCanonicalUserId(habit.userId)
        val entity = HabitEntity.fromDomain(habit.copy(userId = canonicalUserId))
        return jpaRepository.save(entity).toDomain()
    }

    override fun findById(id: UUID): Habit? {
        return jpaRepository.findById(id).orElse(null)?.toDomain()
    }

    override fun findByUserId(userId: String, includeArchived: Boolean): List<Habit> {
        val candidateIds = userIdentifierResolver.resolveAllCandidateIds(userId)
        val entities = if (includeArchived) {
            jpaRepository.findByUserIdInOrderByCreatedAtAsc(candidateIds)
        } else {
            jpaRepository.findByUserIdInAndIsActiveTrueOrderByCreatedAtAsc(candidateIds)
        }
        return entities.map { it.toDomain() }
    }

    override fun findByLinkedTodoCategoryId(userId: String, categoryId: UUID): List<Habit> {
        val candidateIds = userIdentifierResolver.resolveAllCandidateIds(userId)
        return jpaRepository.findByUserIdInAndLinkedTodoCategoryId(candidateIds, categoryId)
            .map { it.toDomain() }
    }

    @Transactional
    override fun deleteById(id: UUID) {
        jpaRepository.deleteById(id)
    }
}

@Component
class HabitExecutionLogRepositoryImpl(
    private val jpaRepository: SpringDataHabitExecutionLogRepository,
    private val userIdentifierResolver: UserIdentifierResolver
) : HabitExecutionLogRepository {

    override fun save(log: HabitExecutionLog): HabitExecutionLog {
        val canonicalUserId = userIdentifierResolver.resolveCanonicalUserId(log.userId)
        val entity = HabitExecutionLogEntity.fromDomain(log.copy(userId = canonicalUserId))
        return jpaRepository.save(entity).toDomain()
    }

    override fun findByHabitIdAndDate(habitId: UUID, date: LocalDate): List<HabitExecutionLog> {
        return jpaRepository.findByHabitIdAndExecutionDate(habitId, date).map { it.toDomain() }
    }

    override fun findByHabitIdAndDateRange(habitId: UUID, startDate: LocalDate, endDate: LocalDate): List<HabitExecutionLog> {
        return jpaRepository.findByHabitIdAndExecutionDateBetween(habitId, startDate, endDate).map { it.toDomain() }
    }

    override fun findByUserIdAndDateRange(userId: String, startDate: LocalDate, endDate: LocalDate): List<HabitExecutionLog> {
        val candidateIds = userIdentifierResolver.resolveAllCandidateIds(userId)
        return jpaRepository.findByUserIdInAndExecutionDateBetween(candidateIds, startDate, endDate)
            .map { it.toDomain() }
    }

    override fun findByHabitIdAndSourceAndReferenceId(
        habitId: UUID,
        source: HabitLogSource,
        sourceReferenceId: String
    ): HabitExecutionLog? {
        return jpaRepository.findByHabitIdAndSourceAndSourceReferenceId(habitId, source, sourceReferenceId)?.toDomain()
    }

    @Transactional
    override fun deleteByHabitIdAndDateAndSource(habitId: UUID, date: LocalDate, source: HabitLogSource) {
        jpaRepository.deleteByHabitIdAndDateAndSource(habitId, date, source)
    }

    @Transactional
    override fun deleteBySourceAndReferenceId(source: HabitLogSource, sourceReferenceId: String) {
        jpaRepository.deleteBySourceAndReferenceId(source, sourceReferenceId)
    }
}

@Component
class HabitPeriodSkipRepositoryImpl(
    private val jpaRepository: SpringDataHabitPeriodSkipRepository,
    private val userIdentifierResolver: UserIdentifierResolver
) : HabitPeriodSkipRepository {

    override fun save(skip: HabitPeriodSkip): HabitPeriodSkip {
        val canonicalUserId = userIdentifierResolver.resolveCanonicalUserId(skip.userId)
        val entity = HabitPeriodSkipEntity.fromDomain(skip.copy(userId = canonicalUserId))
        return jpaRepository.save(entity).toDomain()
    }

    override fun findByHabitIdAndDate(habitId: UUID, date: LocalDate): HabitPeriodSkip? {
        return jpaRepository.findByHabitIdAndSkipDate(habitId, date)?.toDomain()
    }

    override fun findByHabitIdAndDateRange(habitId: UUID, startDate: LocalDate, endDate: LocalDate): List<HabitPeriodSkip> {
        return jpaRepository.findByHabitIdAndSkipDateBetween(habitId, startDate, endDate).map { it.toDomain() }
    }

    override fun findByUserIdAndDateRange(userId: String, startDate: LocalDate, endDate: LocalDate): List<HabitPeriodSkip> {
        val candidateIds = userIdentifierResolver.resolveAllCandidateIds(userId)
        return jpaRepository.findByUserIdInAndSkipDateBetween(candidateIds, startDate, endDate).map { it.toDomain() }
    }

    @Transactional
    override fun deleteByHabitIdAndDate(habitId: UUID, date: LocalDate) {
        jpaRepository.deleteByHabitIdAndDate(habitId, date)
    }
}

@Component
class TodoCategoryRepositoryImpl(
    private val jpaRepository: SpringDataTodoCategoryRepository,
    private val userIdentifierResolver: UserIdentifierResolver
) : TodoCategoryRepository {

    override fun save(category: TodoCategory): TodoCategory {
        val canonicalUserId = userIdentifierResolver.resolveCanonicalUserId(category.userId)
        val entity = TodoCategoryEntity.fromDomain(category.copy(userId = canonicalUserId))
        return jpaRepository.save(entity).toDomain()
    }

    override fun findById(id: UUID): TodoCategory? {
        return jpaRepository.findById(id).orElse(null)?.toDomain()
    }

    override fun findByUserId(userId: String): List<TodoCategory> {
        val candidateIds = userIdentifierResolver.resolveAllCandidateIds(userId)
        return jpaRepository.findByUserIdInOrderByNameAsc(candidateIds).map { it.toDomain() }
    }

    @Transactional
    override fun deleteById(id: UUID) {
        jpaRepository.deleteById(id)
    }
}

@Component
class TodoItemRepositoryImpl(
    private val jpaRepository: SpringDataTodoItemRepository,
    private val subtaskJpaRepository: SpringDataTodoSubtaskRepository,
    private val userIdentifierResolver: UserIdentifierResolver
) : TodoItemRepository {

    @Transactional
    override fun save(todo: TodoItem): TodoItem {
        val canonicalUserId = userIdentifierResolver.resolveCanonicalUserId(todo.userId)
        val savedTodo = jpaRepository.save(TodoItemEntity.fromDomain(todo.copy(userId = canonicalUserId)))
        subtaskJpaRepository.deleteByTodoId(savedTodo.id)
        val savedSubtasks = todo.subtasks.mapIndexed { idx, st ->
            subtaskJpaRepository.save(
                TodoSubtaskEntity.fromDomain(st.copy(todoId = savedTodo.id, sortOrder = idx))
            ).toDomain()
        }
        return savedTodo.toDomain(savedSubtasks)
    }

    override fun findById(id: UUID): TodoItem? {
        val entity = jpaRepository.findById(id).orElse(null) ?: return null
        val subtasks = subtaskJpaRepository.findByTodoIdOrderBySortOrderAsc(entity.id).map { it.toDomain() }
        return entity.toDomain(subtasks)
    }

    override fun findByUserId(
        userId: String,
        status: TodoStatus?,
        categoryId: UUID?,
        priority: TodoPriority?
    ): List<TodoItem> {
        val candidateIds = userIdentifierResolver.resolveAllCandidateIds(userId)
        val entities = jpaRepository.findByUserIdInOrderByCreatedAtDesc(candidateIds)
            .filter { entity ->
                (status == null || entity.status == status) &&
                    (categoryId == null || entity.categoryId == categoryId) &&
                    (priority == null || entity.priority == priority)
            }
        if (entities.isEmpty()) return emptyList()

        val subtasksByTodoId = subtaskJpaRepository
            .findByTodoIdInOrderBySortOrderAsc(entities.map { it.id })
            .groupBy { it.todoId }

        return entities.map { entity ->
            entity.toDomain(subtasksByTodoId[entity.id].orEmpty().map { it.toDomain() })
        }
    }

    @Transactional
    override fun deleteById(id: UUID) {
        subtaskJpaRepository.deleteByTodoId(id)
        jpaRepository.deleteById(id)
    }
}

@Component
class UserRoutineSettingsRepositoryImpl(
    private val jpaRepository: SpringDataUserRoutineSettingsRepository,
    private val userIdentifierResolver: UserIdentifierResolver
) : UserRoutineSettingsRepository {

    override fun findByUserId(userId: String): UserRoutineSettings {
        val candidateIds = userIdentifierResolver.resolveAllCandidateIds(userId)
        val existing = jpaRepository.findByUserIdIn(candidateIds).firstOrNull()
        if (existing != null) {
            return existing.toDomain()
        }
        val canonicalUserId = userIdentifierResolver.resolveCanonicalUserId(userId)
        return UserRoutineSettings(userId = canonicalUserId)
    }

    override fun save(settings: UserRoutineSettings): UserRoutineSettings {
        val canonicalUserId = userIdentifierResolver.resolveCanonicalUserId(settings.userId)
        val entity = UserRoutineSettingsEntity.fromDomain(settings.copy(userId = canonicalUserId))
        return jpaRepository.save(entity).toDomain()
    }
}
