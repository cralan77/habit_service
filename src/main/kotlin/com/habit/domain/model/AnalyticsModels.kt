package com.habit.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

data class WeekWindow(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val weekStartDay: DayOfWeek
)

data class HabitStreakResult(
    val habitId: UUID,
    val currentStreak: Int,
    val bestStreak: Int,
    val streakUnit: StreakUnit,
    val atRiskToday: Boolean = false
)

data class DailyGauge(
    val fixedExpectedCount: Int,
    val fixedCompletedCount: Int,
    val fixedSkippedCount: Int,
    val fixedSuccessPercentage: Double,
    val flexibleCompletedTodayBonus: Int
)

data class FixedHabitTodayItem(
    val habitId: UUID,
    val title: String,
    val description: String?,
    val icon: String,
    val colorHex: String,
    val scheduleType: ScheduleType,
    val completionMode: CompletionMode,
    val dailyTargetValue: Double,
    val currentValueToday: Double,
    val unit: String?,
    val progressPercentage: Double,
    val statusToday: HabitStatusToday,
    val currentStreak: Int,
    val bestStreak: Int,
    val streakUnit: StreakUnit
)

data class FlexibleHabitInPeriodItem(
    val habitId: UUID,
    val title: String,
    val description: String?,
    val icon: String,
    val colorHex: String,
    val scheduleType: ScheduleType,
    val completionMode: CompletionMode,
    val linkedTodoCategoryId: UUID?,
    val linkedTodoCategoryName: String?,
    val periodLabel: String,
    val periodTargetCount: Int,
    val periodAchievedCount: Int,
    val periodSkippedCount: Int = 0,
    val isPeriodGoalMet: Boolean,
    val isOverachieved: Boolean,
    val executedToday: Boolean,
    val currentStreak: Int,
    val bestStreak: Int,
    val streakUnit: StreakUnit
)

data class UrgentTodoSummaryItem(
    val id: UUID,
    val title: String,
    val categoryId: UUID?,
    val categoryName: String?,
    val linkedHabitId: UUID?,
    val effectiveLinkedHabitTitles: List<String>,
    val priority: TodoPriority,
    val status: TodoStatus,
    val dueDate: LocalDate?,
    val subtasksCompleted: Int,
    val subtasksTotal: Int
)

data class DailyRoutineView(
    val date: LocalDate,
    val dayOfWeek: DayOfWeek,
    val weekWindow: WeekWindow,
    val dailyGauge: DailyGauge,
    val fixedHabitsToday: List<FixedHabitTodayItem>,
    val flexibleHabitsInPeriod: List<FlexibleHabitInPeriodItem>,
    val urgentAndTodayTodos: List<UrgentTodoSummaryItem>
)

data class OverallComparisonMetrics(
    val totalExpected: Int,
    val totalAchievedCapped: Int,
    val totalAchievedRaw: Int,
    val totalSkipped: Int,
    val consistencyPercentage: Double,
    val overachievementCount: Int
)

data class BarChartPoint(
    val label: String,
    val date: LocalDate,
    val fixedExpected: Int,
    val fixedAchieved: Int,
    val flexibleExecutedToday: Int,
    val totalExpectedForBar: Int,
    val totalAchievedForBar: Int,
    val percentage: Double
)

data class HabitBreakdownItem(
    val habitId: UUID,
    val title: String,
    val icon: String,
    val colorHex: String,
    val scheduleType: ScheduleType,
    val completionMode: CompletionMode,
    val expectedInPeriod: Int,
    val achievedRawInPeriod: Int,
    val achievedCappedInPeriod: Int,
    val skippedInPeriod: Int,
    val percentageCapped: Double,
    val percentageRaw: Double,
    val statusBadge: HabitPeriodStatusBadge
)

data class TrendLinePoint(
    val periodLabel: String,
    val startDate: LocalDate,
    val consistencyPercentage: Double
)

data class ExpectedVsAchievedComparison(
    val periodType: PeriodType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val weekStartDay: DayOfWeek,
    val overallMetrics: OverallComparisonMetrics,
    val barChartSeries: List<BarChartPoint>,
    val habitBreakdown: List<HabitBreakdownItem>,
    val trendLineSeries: List<TrendLinePoint>
)

data class HeatmapDayCell(
    val date: LocalDate,
    val dayOfWeek: DayOfWeek,
    val completedCount: Int,
    val expectedCount: Int,
    val intensityLevel: Int, // 0..4
    val percentage: Double
)

data class HabitHeatmapResult(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val months: Int,
    val habitId: UUID?,
    val totalActiveDays: Int,
    val cells: List<HeatmapDayCell>
)

data class RoutineAiSummary(
    val date: LocalDate,
    val fixedExpectedToday: Int,
    val fixedCompletedToday: Int,
    val fixedPendingTodayTitles: List<String>,
    val flexibleCompletedTodayBonus: Int,
    val flexibleAtRiskThisPeriod: List<String>,
    val weeklyConsistencyPercentage: Double,
    val monthlyConsistencyPercentage: Double,
    val pendingTodosCount: Int,
    val todosDueTodayOrOverdueTitles: List<String>,
    val topStreakHabitTitle: String?,
    val topStreakCount: Int
)
