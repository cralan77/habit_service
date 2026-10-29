package com.habit.infrastructure.persistence

import com.habit.domain.model.HabitLogSource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface SpringDataUserRoutineSettingsRepository : JpaRepository<UserRoutineSettingsEntity, String> {
    fun findByUserIdIn(userIds: Collection<String>): List<UserRoutineSettingsEntity>
}

@Repository
interface SpringDataTodoCategoryRepository : JpaRepository<TodoCategoryEntity, UUID> {
    fun findByUserIdInOrderByNameAsc(userIds: Collection<String>): List<TodoCategoryEntity>
}

@Repository
interface SpringDataHabitRepository : JpaRepository<HabitEntity, UUID> {
    fun findByUserIdInOrderByCreatedAtAsc(userIds: Collection<String>): List<HabitEntity>
    fun findByUserIdInAndIsActiveTrueOrderByCreatedAtAsc(userIds: Collection<String>): List<HabitEntity>
    fun findByUserIdInAndLinkedTodoCategoryId(userIds: Collection<String>, categoryId: UUID): List<HabitEntity>
}

@Repository
interface SpringDataHabitExecutionLogRepository : JpaRepository<HabitExecutionLogEntity, UUID> {
    fun findByHabitIdAndExecutionDate(habitId: UUID, executionDate: LocalDate): List<HabitExecutionLogEntity>
    fun findByHabitIdAndExecutionDateBetween(habitId: UUID, startDate: LocalDate, endDate: LocalDate): List<HabitExecutionLogEntity>
    fun findByUserIdInAndExecutionDateBetween(userIds: Collection<String>, startDate: LocalDate, endDate: LocalDate): List<HabitExecutionLogEntity>
    fun findByHabitIdAndSourceAndSourceReferenceId(
        habitId: UUID,
        source: HabitLogSource,
        sourceReferenceId: String
    ): HabitExecutionLogEntity?

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM HabitExecutionLogEntity l WHERE l.habitId = :habitId AND l.executionDate = :date AND l.source = :source")
    fun deleteByHabitIdAndDateAndSource(habitId: UUID, date: LocalDate, source: HabitLogSource)

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM HabitExecutionLogEntity l WHERE l.source = :source AND l.sourceReferenceId = :sourceReferenceId")
    fun deleteBySourceAndReferenceId(source: HabitLogSource, sourceReferenceId: String)
}

@Repository
interface SpringDataHabitPeriodSkipRepository : JpaRepository<HabitPeriodSkipEntity, UUID> {
    fun findByHabitIdAndSkipDate(habitId: UUID, skipDate: LocalDate): HabitPeriodSkipEntity?
    fun findByHabitIdAndSkipDateBetween(habitId: UUID, startDate: LocalDate, endDate: LocalDate): List<HabitPeriodSkipEntity>
    fun findByUserIdInAndSkipDateBetween(userIds: Collection<String>, startDate: LocalDate, endDate: LocalDate): List<HabitPeriodSkipEntity>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM HabitPeriodSkipEntity s WHERE s.habitId = :habitId AND s.skipDate = :skipDate")
    fun deleteByHabitIdAndDate(habitId: UUID, skipDate: LocalDate)
}

@Repository
interface SpringDataTodoItemRepository : JpaRepository<TodoItemEntity, UUID> {
    fun findByUserIdInOrderByCreatedAtDesc(userIds: Collection<String>): List<TodoItemEntity>
}

@Repository
interface SpringDataTodoSubtaskRepository : JpaRepository<TodoSubtaskEntity, UUID> {
    fun findByTodoIdInOrderBySortOrderAsc(todoIds: Collection<UUID>): List<TodoSubtaskEntity>
    fun findByTodoIdOrderBySortOrderAsc(todoId: UUID): List<TodoSubtaskEntity>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM TodoSubtaskEntity s WHERE s.todoId = :todoId")
    fun deleteByTodoId(todoId: UUID)
}
