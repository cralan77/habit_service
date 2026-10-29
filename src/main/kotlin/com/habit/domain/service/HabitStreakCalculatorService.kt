package com.habit.domain.service

import com.habit.domain.model.Habit
import com.habit.domain.model.HabitExecutionLog
import com.habit.domain.model.HabitPeriodSkip
import com.habit.domain.model.HabitStreakResult
import com.habit.domain.model.PeriodType
import com.habit.domain.model.ScheduleType
import com.habit.domain.model.StreakUnit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.max

class HabitStreakCalculatorService(
    private val scheduleEvaluator: HabitScheduleEvaluatorService
) {

    /**
     * Calcula Current Streak e Best Streak para hábitos Fixos (em DAYS) e Flexíveis (em WEEKS ou MONTHS),
     * tratando dias/períodos SKIPPED como transparentes (congelamento de ofensiva).
     */
    fun calculateStreak(
        habit: Habit,
        logs: List<HabitExecutionLog>,
        skips: List<HabitPeriodSkip>,
        referenceDate: LocalDate = LocalDate.now(),
        weekStartDay: DayOfWeek = DayOfWeek.MONDAY
    ): HabitStreakResult {
        return when (habit.scheduleType) {
            ScheduleType.DAILY,
            ScheduleType.FIXED_WEEKLY,
            ScheduleType.FIXED_MONTHLY -> calculateFixedStreak(habit, logs, skips, referenceDate)

            ScheduleType.FLEXIBLE_WEEKLY -> calculateFlexibleWeeklyStreak(habit, logs, skips, referenceDate, weekStartDay)

            ScheduleType.FLEXIBLE_MONTHLY -> calculateFlexibleMonthlyStreak(habit, logs, skips, referenceDate)
        }
    }

    private fun calculateFixedStreak(
        habit: Habit,
        logs: List<HabitExecutionLog>,
        skips: List<HabitPeriodSkip>,
        referenceDate: LocalDate
    ): HabitStreakResult {
        if (referenceDate.isBefore(habit.startDate)) {
            return HabitStreakResult(habit.id, 0, 0, StreakUnit.DAYS, false)
        }

        val logsByDate = logs.groupBy { it.executionDate }
        val skipDates = skips.map { it.skipDate }.toSet()

        var bestStreak = 0
        var runningStreak = 0

        var cursor = habit.startDate
        while (!cursor.isAfter(referenceDate)) {
            if (scheduleEvaluator.isFixedHabitExpectedOnDate(habit, cursor)) {
                val isCompleted = scheduleEvaluator.isHabitCompletedOnDate(habit, logsByDate[cursor].orEmpty())
                val isSkipped = skipDates.contains(cursor)

                if (isCompleted) {
                    runningStreak++
                    bestStreak = max(bestStreak, runningStreak)
                } else if (isSkipped) {
                    // Transparent: preserves runningStreak without incrementing
                } else if (cursor.isEqual(referenceDate)) {
                    // Today is still in progress: do not reset runningStreak yet
                } else {
                    runningStreak = 0
                }
            }
            cursor = cursor.plusDays(1)
        }

        val expectedToday = scheduleEvaluator.isFixedHabitExpectedOnDate(habit, referenceDate)
        val completedToday = scheduleEvaluator.isHabitCompletedOnDate(habit, logsByDate[referenceDate].orEmpty())
        val skippedToday = skipDates.contains(referenceDate)
        val atRiskToday = expectedToday && !completedToday && !skippedToday && runningStreak > 0

        return HabitStreakResult(
            habitId = habit.id,
            currentStreak = runningStreak,
            bestStreak = bestStreak,
            streakUnit = StreakUnit.DAYS,
            atRiskToday = atRiskToday
        )
    }

    private fun calculateFlexibleWeeklyStreak(
        habit: Habit,
        logs: List<HabitExecutionLog>,
        skips: List<HabitPeriodSkip>,
        referenceDate: LocalDate,
        weekStartDay: DayOfWeek
    ): HabitStreakResult {
        if (referenceDate.isBefore(habit.startDate)) {
            return HabitStreakResult(habit.id, 0, 0, StreakUnit.WEEKS, false)
        }

        val firstWeek = scheduleEvaluator.getWeekWindow(habit.startDate, weekStartDay)
        val currentWeek = scheduleEvaluator.getWeekWindow(referenceDate, weekStartDay)

        var bestStreak = 0
        var runningStreak = 0
        var atRiskToday = false

        var weekStart = firstWeek.startDate
        while (!weekStart.isAfter(currentWeek.startDate)) {
            val weekEnd = weekStart.plusDays(6)
            val isCurrentWeek = weekStart.isEqual(currentWeek.startDate)

            val skipsInWeek = skips.filter { !it.skipDate.isBefore(weekStart) && !it.skipDate.isAfter(weekEnd) }
            val totalSkips = skipsInWeek.sumOf { it.skipCount }
            val expected = max(0, habit.targetCountPerPeriod - totalSkips)
            val achieved = scheduleEvaluator.countAchievedInRange(habit, logs, weekStart, weekEnd)

            if (expected == 0 && totalSkips > 0) {
                // Entire week quota was skipped/frozen: preserve streak
            } else if (achieved >= expected && expected > 0) {
                runningStreak++
                bestStreak = max(bestStreak, runningStreak)
            } else if (isCurrentWeek) {
                // Current week still in progress: keep streak from previous weeks alive
                val remainingDaysInWeek = java.time.temporal.ChronoUnit.DAYS.between(referenceDate, weekEnd).toInt() + 1
                val missing = expected - achieved
                if (missing >= remainingDaysInWeek && runningStreak > 0) {
                    atRiskToday = true
                }
            } else {
                runningStreak = 0
            }

            weekStart = weekStart.plusDays(7)
        }

        return HabitStreakResult(
            habitId = habit.id,
            currentStreak = runningStreak,
            bestStreak = bestStreak,
            streakUnit = StreakUnit.WEEKS,
            atRiskToday = atRiskToday
        )
    }

    private fun calculateFlexibleMonthlyStreak(
        habit: Habit,
        logs: List<HabitExecutionLog>,
        skips: List<HabitPeriodSkip>,
        referenceDate: LocalDate
    ): HabitStreakResult {
        if (referenceDate.isBefore(habit.startDate)) {
            return HabitStreakResult(habit.id, 0, 0, StreakUnit.MONTHS, false)
        }

        var ym = YearMonth.from(habit.startDate)
        val currentYm = YearMonth.from(referenceDate)

        var bestStreak = 0
        var runningStreak = 0
        var atRiskToday = false

        while (!ym.isAfter(currentYm)) {
            val monthStart = ym.atDay(1)
            val monthEnd = ym.atEndOfMonth()
            val isCurrentMonth = ym == currentYm

            val skipsInMonth = skips.filter { !it.skipDate.isBefore(monthStart) && !it.skipDate.isAfter(monthEnd) }
            val totalSkips = skipsInMonth.sumOf { it.skipCount }
            val expected = scheduleEvaluator.countExpectedInPeriod(
                habit = habit,
                periodType = PeriodType.MONTH,
                startDate = monthStart,
                endDate = monthEnd,
                skipsInRange = skipsInMonth
            )
            val achieved = scheduleEvaluator.countAchievedInRange(habit, logs, monthStart, monthEnd)

            if (expected == 0 && totalSkips > 0) {
                // Entire month quota skipped: preserve streak
            } else if (achieved >= expected && expected > 0) {
                runningStreak++
                bestStreak = max(bestStreak, runningStreak)
            } else if (isCurrentMonth) {
                // Current month still in progress: preserve streak from prior months
                val remainingDays = java.time.temporal.ChronoUnit.DAYS.between(referenceDate, monthEnd).toInt() + 1
                if (remainingDays <= 3 && achieved < expected) {
                    atRiskToday = true
                }
            } else {
                runningStreak = 0
            }

            ym = ym.plusMonths(1)
        }

        return HabitStreakResult(
            habitId = habit.id,
            currentStreak = runningStreak,
            bestStreak = bestStreak,
            streakUnit = StreakUnit.MONTHS,
            atRiskToday = atRiskToday
        )
    }
}
