package com.habit.application.analytics

import com.habit.domain.model.ExpectedVsAchievedComparison
import com.habit.domain.model.PeriodType
import com.habit.domain.repository.HabitExecutionLogRepository
import com.habit.domain.repository.HabitPeriodSkipRepository
import com.habit.domain.repository.HabitRepository
import com.habit.domain.repository.UserRoutineSettingsRepository
import com.habit.domain.service.HabitAnalyticsEngineService
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

open class GetExpectedVsAchievedUseCase(
    private val habitRepository: HabitRepository,
    private val logRepository: HabitExecutionLogRepository,
    private val skipRepository: HabitPeriodSkipRepository,
    private val settingsRepository: UserRoutineSettingsRepository,
    private val analyticsEngine: HabitAnalyticsEngineService
) {

    @Transactional(readOnly = true)
    open fun execute(
        userId: String,
        period: PeriodType = PeriodType.WEEK,
        referenceDate: LocalDate = LocalDate.now()
    ): ExpectedVsAchievedComparison {
        val settings = settingsRepository.findByUserId(userId)
        val habits = habitRepository.findByUserId(userId, includeArchived = false)
        val startRange = referenceDate.minusMonths(7).withDayOfMonth(1)
        val endRange = referenceDate.plusMonths(1)

        val logs = logRepository.findByUserIdAndDateRange(userId, startRange, endRange)
        val skips = skipRepository.findByUserIdAndDateRange(userId, startRange, endRange)

        return analyticsEngine.buildExpectedVsAchievedComparison(
            periodType = period,
            referenceDate = referenceDate,
            weekStartDay = settings.weekStartDay,
            habits = habits,
            logs = logs,
            skips = skips
        )
    }
}
