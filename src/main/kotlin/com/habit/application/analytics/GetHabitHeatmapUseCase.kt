package com.habit.application.analytics

import com.habit.domain.model.HabitHeatmapResult
import com.habit.domain.repository.HabitExecutionLogRepository
import com.habit.domain.repository.HabitPeriodSkipRepository
import com.habit.domain.repository.HabitRepository
import com.habit.domain.service.HabitAnalyticsEngineService
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

open class GetHabitHeatmapUseCase(
    private val habitRepository: HabitRepository,
    private val logRepository: HabitExecutionLogRepository,
    private val skipRepository: HabitPeriodSkipRepository,
    private val analyticsEngine: HabitAnalyticsEngineService
) {

    @Transactional(readOnly = true)
    open fun execute(
        userId: String,
        months: Int = 3,
        habitId: UUID? = null,
        referenceDate: LocalDate = LocalDate.now()
    ): HabitHeatmapResult {
        val clampedMonths = months.coerceIn(1, 12)
        val startDate = referenceDate.minusMonths(clampedMonths.toLong()).withDayOfMonth(1)
        val habits = habitRepository.findByUserId(userId, includeArchived = false)
        val logs = logRepository.findByUserIdAndDateRange(userId, startDate, referenceDate)
        val skips = skipRepository.findByUserIdAndDateRange(userId, startDate, referenceDate)

        return analyticsEngine.buildHeatmap(
            months = clampedMonths,
            referenceDate = referenceDate,
            habitId = habitId,
            habits = habits,
            logs = logs,
            skips = skips
        )
    }
}
