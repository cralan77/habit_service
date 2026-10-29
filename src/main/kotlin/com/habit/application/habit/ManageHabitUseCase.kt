package com.habit.application.habit

import com.habit.domain.model.CompletionMode
import com.habit.domain.model.Habit
import com.habit.domain.model.HealthTriggerType
import com.habit.domain.model.PeriodType
import com.habit.domain.model.QuotaCountMode
import com.habit.domain.model.ScheduleType
import com.habit.domain.model.StreakUnit
import com.habit.domain.repository.HabitExecutionLogRepository
import com.habit.domain.repository.HabitPeriodSkipRepository
import com.habit.domain.repository.HabitRepository
import com.habit.domain.repository.TodoCategoryRepository
import com.habit.domain.repository.UserRoutineSettingsRepository
import com.habit.domain.service.HabitScheduleEvaluatorService
import com.habit.domain.service.HabitStreakCalculatorService
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class HabitWithStats(
    val habit: Habit,
    val linkedTodoCategoryName: String?,
    val currentStreak: Int,
    val bestStreak: Int,
    val streakUnit: StreakUnit,
    val periodTargetCount: Int,
    val periodAchievedCount: Int,
    val completedToday: Boolean,
    val currentValueToday: Double
)

open class ManageHabitUseCase(
    private val habitRepository: HabitRepository,
    private val logRepository: HabitExecutionLogRepository,
    private val skipRepository: HabitPeriodSkipRepository,
    private val categoryRepository: TodoCategoryRepository,
    private val settingsRepository: UserRoutineSettingsRepository,
    private val scheduleEvaluator: HabitScheduleEvaluatorService,
    private val streakCalculator: HabitStreakCalculatorService
) {

    @Transactional
    open fun createHabit(
        userId: String,
        title: String,
        description: String?,
        icon: String?,
        colorHex: String?,
        scheduleType: ScheduleType,
        daysOfWeek: Set<DayOfWeek>?,
        daysOfMonth: Set<Int>?,
        targetCountPerPeriod: Int?,
        quotaCountMode: QuotaCountMode?,
        completionMode: CompletionMode?,
        dailyTargetValue: Double?,
        unit: String?,
        linkedTodoCategoryId: UUID?,
        healthTriggerType: HealthTriggerType?,
        healthExerciseType: String?,
        healthMinDurationMinutes: Int?,
        startDate: LocalDate?
    ): HabitWithStats {
        require(title.isNotBlank()) { "O título do hábito é obrigatório" }
        val resolvedMode = completionMode ?: CompletionMode.CHECK
        val resolvedQuotaMode = quotaCountMode ?: if (resolvedMode == CompletionMode.TODO_LINKED) {
            QuotaCountMode.TOTAL_EXECUTIONS
        } else {
            QuotaCountMode.DAYS_COMPLETED
        }

        val resolvedTargetCount = when (scheduleType) {
            ScheduleType.DAILY -> 1
            ScheduleType.FIXED_WEEKLY -> (daysOfWeek?.size ?: 1).coerceAtLeast(1)
            ScheduleType.FIXED_MONTHLY -> (daysOfMonth?.size ?: 1).coerceAtLeast(1)
            ScheduleType.FLEXIBLE_WEEKLY -> (targetCountPerPeriod ?: 1).coerceIn(1, 31)
            ScheduleType.FLEXIBLE_MONTHLY -> (targetCountPerPeriod ?: 1).coerceIn(1, 100)
        }

        val habit = Habit(
            id = UUID.randomUUID(),
            userId = userId,
            title = title.trim(),
            description = description?.trim(),
            icon = icon?.takeIf { it.isNotBlank() } ?: "check-circle",
            colorHex = colorHex?.takeIf { it.isNotBlank() } ?: "#10B981",
            scheduleType = scheduleType,
            daysOfWeek = daysOfWeek ?: emptySet(),
            daysOfMonth = daysOfMonth ?: emptySet(),
            targetCountPerPeriod = resolvedTargetCount,
            quotaCountMode = resolvedQuotaMode,
            completionMode = resolvedMode,
            dailyTargetValue = (dailyTargetValue ?: 1.0).coerceAtLeast(0.1),
            unit = unit?.trim(),
            linkedTodoCategoryId = linkedTodoCategoryId,
            healthTriggerType = healthTriggerType,
            healthExerciseType = healthExerciseType?.trim(),
            healthMinDurationMinutes = healthMinDurationMinutes,
            isActive = true,
            startDate = startDate ?: LocalDate.now(),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val saved = habitRepository.save(habit)
        return enrichHabit(saved, userId)
    }

    @Transactional
    open fun updateHabit(
        userId: String,
        habitId: UUID,
        title: String,
        description: String?,
        icon: String?,
        colorHex: String?,
        scheduleType: ScheduleType,
        daysOfWeek: Set<DayOfWeek>?,
        daysOfMonth: Set<Int>?,
        targetCountPerPeriod: Int?,
        quotaCountMode: QuotaCountMode?,
        completionMode: CompletionMode?,
        dailyTargetValue: Double?,
        unit: String?,
        linkedTodoCategoryId: UUID?,
        healthTriggerType: HealthTriggerType?,
        healthExerciseType: String?,
        healthMinDurationMinutes: Int?
    ): HabitWithStats {
        val existing = habitRepository.findById(habitId)
            ?: throw NoSuchElementException("Hábito não encontrado: $habitId")

        val resolvedMode = completionMode ?: existing.completionMode
        val resolvedQuotaMode = quotaCountMode ?: existing.quotaCountMode
        val resolvedTargetCount = when (scheduleType) {
            ScheduleType.DAILY -> 1
            ScheduleType.FIXED_WEEKLY -> (daysOfWeek?.size ?: existing.daysOfWeek.size).coerceAtLeast(1)
            ScheduleType.FIXED_MONTHLY -> (daysOfMonth?.size ?: existing.daysOfMonth.size).coerceAtLeast(1)
            ScheduleType.FLEXIBLE_WEEKLY, ScheduleType.FLEXIBLE_MONTHLY ->
                (targetCountPerPeriod ?: existing.targetCountPerPeriod).coerceAtLeast(1)
        }

        val updated = existing.copy(
            title = title.trim(),
            description = description?.trim(),
            icon = icon?.takeIf { it.isNotBlank() } ?: existing.icon,
            colorHex = colorHex?.takeIf { it.isNotBlank() } ?: existing.colorHex,
            scheduleType = scheduleType,
            daysOfWeek = daysOfWeek ?: existing.daysOfWeek,
            daysOfMonth = daysOfMonth ?: existing.daysOfMonth,
            targetCountPerPeriod = resolvedTargetCount,
            quotaCountMode = resolvedQuotaMode,
            completionMode = resolvedMode,
            dailyTargetValue = (dailyTargetValue ?: existing.dailyTargetValue).coerceAtLeast(0.1),
            unit = unit?.trim(),
            linkedTodoCategoryId = linkedTodoCategoryId,
            healthTriggerType = healthTriggerType,
            healthExerciseType = healthExerciseType?.trim(),
            healthMinDurationMinutes = healthMinDurationMinutes,
            updatedAt = Instant.now()
        )

        val saved = habitRepository.save(updated)
        return enrichHabit(saved, userId)
    }

    @Transactional
    open fun archiveOrRestoreHabit(userId: String, habitId: UUID, archive: Boolean? = null): HabitWithStats {
        val existing = habitRepository.findById(habitId)
            ?: throw NoSuchElementException("Hábito não encontrado: $habitId")

        val nextActive = if (archive != null) !archive else !existing.isActive
        val updated = existing.copy(
            isActive = nextActive,
            archivedAt = if (nextActive) null else Instant.now(),
            updatedAt = Instant.now()
        )
        val saved = habitRepository.save(updated)
        return enrichHabit(saved, userId)
    }

    @Transactional
    open fun deleteHabit(userId: String, habitId: UUID) {
        habitRepository.findById(habitId)
            ?: throw NoSuchElementException("Hábito não encontrado: $habitId")
        habitRepository.deleteById(habitId)
    }

    @Transactional(readOnly = true)
    open fun getHabitById(userId: String, habitId: UUID): HabitWithStats {
        val habit = habitRepository.findById(habitId)
            ?: throw NoSuchElementException("Hábito não encontrado: $habitId")
        return enrichHabit(habit, userId)
    }

    @Transactional(readOnly = true)
    open fun listHabits(userId: String, includeArchived: Boolean = false): List<HabitWithStats> {
        val habits = habitRepository.findByUserId(userId, includeArchived)
        if (habits.isEmpty()) return emptyList()

        val today = LocalDate.now()
        val settings = settingsRepository.findByUserId(userId)
        val categoriesById = categoryRepository.findByUserId(userId).associateBy { it.id }

        val earliestStart = habits.minOf { it.startDate }.coerceAtMost(today.minusMonths(6))
        val allLogs = logRepository.findByUserIdAndDateRange(userId, earliestStart, today.plusDays(7))
        val allSkips = skipRepository.findByUserIdAndDateRange(userId, earliestStart, today.plusDays(7))
        val logsByHabit = allLogs.groupBy { it.habitId }
        val skipsByHabit = allSkips.groupBy { it.habitId }

        return habits.map { habit ->
            buildHabitStats(
                habit = habit,
                logs = logsByHabit[habit.id].orEmpty(),
                skips = skipsByHabit[habit.id].orEmpty(),
                categoryName = habit.linkedTodoCategoryId?.let { categoriesById[it]?.name },
                weekStartDay = settings.weekStartDay,
                today = today
            )
        }
    }

    private fun enrichHabit(habit: Habit, userId: String): HabitWithStats {
        val today = LocalDate.now()
        val settings = settingsRepository.findByUserId(userId)
        val earliestStart = habit.startDate.coerceAtMost(today.minusMonths(6))
        val logs = logRepository.findByHabitIdAndDateRange(habit.id, earliestStart, today.plusDays(7))
        val skips = skipRepository.findByHabitIdAndDateRange(habit.id, earliestStart, today.plusDays(7))
        val categoryName = habit.linkedTodoCategoryId?.let { categoryRepository.findById(it)?.name }

        return buildHabitStats(
            habit = habit,
            logs = logs,
            skips = skips,
            categoryName = categoryName,
            weekStartDay = settings.weekStartDay,
            today = today
        )
    }

    private fun buildHabitStats(
        habit: Habit,
        logs: List<com.habit.domain.model.HabitExecutionLog>,
        skips: List<com.habit.domain.model.HabitPeriodSkip>,
        categoryName: String?,
        weekStartDay: DayOfWeek,
        today: LocalDate
    ): HabitWithStats {
        val streak = streakCalculator.calculateStreak(habit, logs, skips, today, weekStartDay)
        val logsToday = logs.filter { it.executionDate.isEqual(today) }
        val completedToday = scheduleEvaluator.isHabitCompletedOnDate(habit, logsToday)
        val valueToday = scheduleEvaluator.getDailyValue(habit, logsToday)

        val (periodStart, periodEnd, periodType) = when (habit.scheduleType) {
            ScheduleType.FLEXIBLE_MONTHLY, ScheduleType.FIXED_MONTHLY -> {
                val m = scheduleEvaluator.getMonthWindow(today)
                Triple(m.first, m.second, PeriodType.MONTH)
            }
            else -> {
                val w = scheduleEvaluator.getWeekWindow(today, weekStartDay)
                Triple(w.startDate, w.endDate, PeriodType.WEEK)
            }
        }

        val skipsInPeriod = skips.filter { !it.skipDate.isBefore(periodStart) && !it.skipDate.isAfter(periodEnd) }
        val periodTarget = scheduleEvaluator.countExpectedInPeriod(
            habit = habit,
            periodType = periodType,
            startDate = periodStart,
            endDate = periodEnd,
            skipsInRange = skipsInPeriod,
            weekStartDay = weekStartDay
        )
        val periodAchieved = scheduleEvaluator.countAchievedInRange(habit, logs, periodStart, periodEnd)

        return HabitWithStats(
            habit = habit,
            linkedTodoCategoryName = categoryName,
            currentStreak = streak.currentStreak,
            bestStreak = streak.bestStreak,
            streakUnit = streak.streakUnit,
            periodTargetCount = periodTarget,
            periodAchievedCount = periodAchieved,
            completedToday = completedToday,
            currentValueToday = valueToday
        )
    }
}
