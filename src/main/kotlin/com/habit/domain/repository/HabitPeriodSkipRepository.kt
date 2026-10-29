package com.habit.domain.repository

import com.habit.domain.model.HabitPeriodSkip
import java.time.LocalDate
import java.util.UUID

interface HabitPeriodSkipRepository {
    fun save(skip: HabitPeriodSkip): HabitPeriodSkip
    fun findByHabitIdAndDate(habitId: UUID, date: LocalDate): HabitPeriodSkip?
    fun findByHabitIdAndDateRange(habitId: UUID, startDate: LocalDate, endDate: LocalDate): List<HabitPeriodSkip>
    fun findByUserIdAndDateRange(userId: String, startDate: LocalDate, endDate: LocalDate): List<HabitPeriodSkip>
    fun deleteByHabitIdAndDate(habitId: UUID, date: LocalDate)
}
