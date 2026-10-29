package com.habit.application.habit

import com.habit.domain.model.CompletionMode
import com.habit.domain.model.HabitExecutionLog
import com.habit.domain.model.HabitLogSource
import com.habit.domain.model.HabitLogStatus
import com.habit.domain.model.HabitPeriodSkip
import com.habit.domain.repository.HabitExecutionLogRepository
import com.habit.domain.repository.HabitPeriodSkipRepository
import com.habit.domain.repository.HabitRepository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

open class LogHabitExecutionUseCase(
    private val habitRepository: HabitRepository,
    private val logRepository: HabitExecutionLogRepository,
    private val skipRepository: HabitPeriodSkipRepository,
    private val manageHabitUseCase: ManageHabitUseCase
) {

    @Transactional
    open fun checkHabit(
        userId: String,
        habitId: UUID,
        date: LocalDate = LocalDate.now(),
        value: Double? = null,
        notes: String? = null,
        source: HabitLogSource = HabitLogSource.MANUAL
    ): HabitWithStats {
        val habit = habitRepository.findById(habitId)
            ?: throw NoSuchElementException("Hábito não encontrado: $habitId")

        val existingLogs = logRepository.findByHabitIdAndDate(habitId, date)
            .filter { it.source == HabitLogSource.MANUAL || it.source == HabitLogSource.AI_ASSISTANT }

        if (habit.completionMode == CompletionMode.QUANTITATIVE) {
            val delta = value ?: habit.dailyTargetValue
            val currentManualLog = existingLogs.firstOrNull()
            if (currentManualLog != null) {
                val newTotal = (currentManualLog.completedValue + delta).coerceAtLeast(0.0)
                val newStatus = if (newTotal >= habit.dailyTargetValue) HabitLogStatus.COMPLETED else HabitLogStatus.PARTIAL
                logRepository.save(
                    currentManualLog.copy(
                        completedValue = newTotal,
                        status = newStatus,
                        notes = notes ?: currentManualLog.notes,
                        updatedAt = Instant.now()
                    )
                )
            } else {
                val totalValue = delta.coerceAtLeast(0.0)
                val status = if (totalValue >= habit.dailyTargetValue) HabitLogStatus.COMPLETED else HabitLogStatus.PARTIAL
                logRepository.save(
                    HabitExecutionLog(
                        id = UUID.randomUUID(),
                        habitId = habit.id,
                        userId = habit.userId,
                        executionDate = date,
                        completedValue = totalValue,
                        status = status,
                        source = source,
                        notes = notes,
                        createdAt = Instant.now(),
                        updatedAt = Instant.now()
                    )
                )
            }
        } else {
            val currentManualLog = existingLogs.firstOrNull()
            val targetVal = value ?: habit.dailyTargetValue.coerceAtLeast(1.0)
            if (currentManualLog != null) {
                logRepository.save(
                    currentManualLog.copy(
                        completedValue = targetVal,
                        status = HabitLogStatus.COMPLETED,
                        notes = notes ?: currentManualLog.notes,
                        updatedAt = Instant.now()
                    )
                )
            } else {
                logRepository.save(
                    HabitExecutionLog(
                        id = UUID.randomUUID(),
                        habitId = habit.id,
                        userId = habit.userId,
                        executionDate = date,
                        completedValue = targetVal,
                        status = HabitLogStatus.COMPLETED,
                        source = source,
                        notes = notes,
                        createdAt = Instant.now(),
                        updatedAt = Instant.now()
                    )
                )
            }
        }

        // Se o dia estava marcado como SKIPPED, remove o skip pois foi concluído
        skipRepository.deleteByHabitIdAndDate(habitId, date)

        return manageHabitUseCase.getHabitById(userId, habitId)
    }

    @Transactional
    open fun uncheckHabit(
        userId: String,
        habitId: UUID,
        date: LocalDate = LocalDate.now()
    ): HabitWithStats {
        habitRepository.findById(habitId)
            ?: throw NoSuchElementException("Hábito não encontrado: $habitId")

        logRepository.deleteByHabitIdAndDateAndSource(habitId, date, HabitLogSource.MANUAL)
        logRepository.deleteByHabitIdAndDateAndSource(habitId, date, HabitLogSource.AI_ASSISTANT)

        return manageHabitUseCase.getHabitById(userId, habitId)
    }

    @Transactional
    open fun skipHabit(
        userId: String,
        habitId: UUID,
        date: LocalDate = LocalDate.now(),
        skipCount: Int = 1,
        reason: String? = null
    ): HabitWithStats {
        val habit = habitRepository.findById(habitId)
            ?: throw NoSuchElementException("Hábito não encontrado: $habitId")

        val existing = skipRepository.findByHabitIdAndDate(habitId, date)
        if (existing != null) {
            skipRepository.save(
                existing.copy(
                    skipCount = skipCount.coerceAtLeast(1),
                    reason = reason ?: existing.reason
                )
            )
        } else {
            skipRepository.save(
                HabitPeriodSkip(
                    id = UUID.randomUUID(),
                    habitId = habit.id,
                    userId = habit.userId,
                    skipDate = date,
                    skipCount = skipCount.coerceAtLeast(1),
                    reason = reason,
                    createdAt = Instant.now()
                )
            )
        }

        return manageHabitUseCase.getHabitById(userId, habitId)
    }

    @Transactional
    open fun unskipHabit(
        userId: String,
        habitId: UUID,
        date: LocalDate = LocalDate.now()
    ): HabitWithStats {
        habitRepository.findById(habitId)
            ?: throw NoSuchElementException("Hábito não encontrado: $habitId")

        skipRepository.deleteByHabitIdAndDate(habitId, date)
        return manageHabitUseCase.getHabitById(userId, habitId)
    }
}
