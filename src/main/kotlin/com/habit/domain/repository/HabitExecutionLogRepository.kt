package com.habit.domain.repository

import com.habit.domain.model.HabitExecutionLog
import com.habit.domain.model.HabitLogSource
import java.time.LocalDate
import java.util.UUID

interface HabitExecutionLogRepository {
    fun save(log: HabitExecutionLog): HabitExecutionLog
    fun findByHabitIdAndDate(habitId: UUID, date: LocalDate): List<HabitExecutionLog>
    fun findByHabitIdAndDateRange(habitId: UUID, startDate: LocalDate, endDate: LocalDate): List<HabitExecutionLog>
    fun findByUserIdAndDateRange(userId: String, startDate: LocalDate, endDate: LocalDate): List<HabitExecutionLog>
    fun findByHabitIdAndSourceAndReferenceId(habitId: UUID, source: HabitLogSource, sourceReferenceId: String): HabitExecutionLog?
    fun deleteByHabitIdAndDateAndSource(habitId: UUID, date: LocalDate, source: HabitLogSource)
    fun deleteBySourceAndReferenceId(source: HabitLogSource, sourceReferenceId: String)
}
