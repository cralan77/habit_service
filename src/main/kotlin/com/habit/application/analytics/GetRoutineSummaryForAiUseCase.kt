package com.habit.application.analytics

import com.habit.domain.model.HabitStatusToday
import com.habit.domain.model.PeriodType
import com.habit.domain.model.RoutineAiSummary
import com.habit.domain.model.TodoStatus
import com.habit.domain.repository.TodoItemRepository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

open class GetRoutineSummaryForAiUseCase(
    private val getDailyRoutineViewUseCase: GetDailyRoutineViewUseCase,
    private val getExpectedVsAchievedUseCase: GetExpectedVsAchievedUseCase,
    private val todoRepository: TodoItemRepository
) {

    @Transactional(readOnly = true)
    open fun execute(userId: String, date: LocalDate = LocalDate.now()): RoutineAiSummary {
        val dailyView = getDailyRoutineViewUseCase.execute(userId, date)
        val weeklyComparison = getExpectedVsAchievedUseCase.execute(userId, PeriodType.WEEK, date)
        val monthlyComparison = getExpectedVsAchievedUseCase.execute(userId, PeriodType.MONTH, date)
        val pendingTodos = todoRepository.findByUserId(userId, status = TodoStatus.PENDING)

        val fixedPendingTitles = dailyView.fixedHabitsToday
            .filter { it.statusToday == HabitStatusToday.PENDING || it.statusToday == HabitStatusToday.PARTIAL }
            .map { it.title }

        val flexibleAtRisk = dailyView.flexibleHabitsInPeriod
            .filter { !it.isPeriodGoalMet }
            .map { "${it.title} (${it.periodAchievedCount}/${it.periodTargetCount} em ${it.periodLabel})" }

        val dueTodayOrOverdue = pendingTodos
            .filter { it.dueDate != null && !it.dueDate.isAfter(date) }
            .map { it.title }

        val allStreaks = dailyView.fixedHabitsToday.map { it.title to it.currentStreak } +
            dailyView.flexibleHabitsInPeriod.map { it.title to it.currentStreak }
        val topStreak = allStreaks.maxByOrNull { it.second }

        return RoutineAiSummary(
            date = date,
            fixedExpectedToday = dailyView.dailyGauge.fixedExpectedCount,
            fixedCompletedToday = dailyView.dailyGauge.fixedCompletedCount,
            fixedPendingTodayTitles = fixedPendingTitles,
            flexibleCompletedTodayBonus = dailyView.dailyGauge.flexibleCompletedTodayBonus,
            flexibleAtRiskThisPeriod = flexibleAtRisk,
            weeklyConsistencyPercentage = weeklyComparison.overallMetrics.consistencyPercentage,
            monthlyConsistencyPercentage = monthlyComparison.overallMetrics.consistencyPercentage,
            pendingTodosCount = pendingTodos.size,
            todosDueTodayOrOverdueTitles = dueTodayOrOverdue,
            topStreakHabitTitle = topStreak?.first,
            topStreakCount = topStreak?.second ?: 0
        )
    }
}
