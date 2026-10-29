package com.habit.domain.service

import com.habit.domain.model.CompletionMode
import com.habit.domain.model.Habit
import com.habit.domain.model.HabitExecutionLog
import com.habit.domain.model.HabitLogStatus
import com.habit.domain.model.HabitPeriodSkip
import com.habit.domain.model.HabitPeriodStatusBadge
import com.habit.domain.model.HabitStatusToday
import com.habit.domain.model.PeriodType
import com.habit.domain.model.ScheduleType
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HabitAnalyticsEngineServiceTest {

    private val scheduleEvaluator = HabitScheduleEvaluatorService()
    private val streakCalculator = HabitStreakCalculatorService(scheduleEvaluator)
    private val engine = HabitAnalyticsEngineService(scheduleEvaluator, streakCalculator)

    @Test
    fun `deve separar Habitos Fixos do Dia e Habitos Flexiveis na Visao Diaria`() {
        val today = LocalDate.of(2026, 10, 28) // Wednesday
        val fixedHabit = Habit(
            userId = "u1",
            title = "Beber 2000ml de Agua",
            scheduleType = ScheduleType.DAILY,
            completionMode = CompletionMode.QUANTITATIVE,
            dailyTargetValue = 2000.0,
            unit = "ml",
            startDate = LocalDate.of(2026, 10, 1)
        )
        val flexibleHabit = Habit(
            userId = "u1",
            title = "Treinar 3x na semana",
            scheduleType = ScheduleType.FLEXIBLE_WEEKLY,
            targetCountPerPeriod = 3,
            startDate = LocalDate.of(2026, 10, 1)
        )

        val logs = listOf(
            HabitExecutionLog(
                habitId = fixedHabit.id,
                userId = "u1",
                executionDate = today,
                completedValue = 2000.0,
                status = HabitLogStatus.COMPLETED
            ),
            HabitExecutionLog(
                habitId = flexibleHabit.id,
                userId = "u1",
                executionDate = today,
                completedValue = 1.0,
                status = HabitLogStatus.COMPLETED
            )
        )

        val view = engine.buildDailyRoutineView(
            date = today,
            weekStartDay = DayOfWeek.MONDAY,
            habits = listOf(fixedHabit, flexibleHabit),
            logs = logs,
            skips = emptyList(),
            todos = emptyList(),
            categoriesById = emptyMap()
        )

        view.fixedHabitsToday.shouldHaveSize(1)
        view.fixedHabitsToday.first().title shouldBe "Beber 2000ml de Agua"
        view.fixedHabitsToday.first().statusToday shouldBe HabitStatusToday.COMPLETED

        view.flexibleHabitsInPeriod.shouldHaveSize(1)
        view.flexibleHabitsInPeriod.first().title shouldBe "Treinar 3x na semana"
        view.flexibleHabitsInPeriod.first().executedToday shouldBe true
        view.flexibleHabitsInPeriod.first().periodAchievedCount shouldBe 1
        view.flexibleHabitsInPeriod.first().periodTargetCount shouldBe 3

        view.dailyGauge.fixedExpectedCount shouldBe 1
        view.dailyGauge.fixedCompletedCount shouldBe 1
        view.dailyGauge.fixedSuccessPercentage shouldBe 100.0
        view.dailyGauge.flexibleCompletedTodayBonus shouldBe 1
    }

    @Test
    fun `deve aplicar cap de 100 porcento na taxa geral e preservar OVERACHIEVED quando habito faz 4 de 3`() {
        val refDate = LocalDate.of(2026, 10, 28) // Week: 2026-10-26..2026-11-01
        val weekStart = LocalDate.of(2026, 10, 26)

        val habitA = Habit(
            userId = "u1",
            title = "Habito A (Meta 3x)",
            scheduleType = ScheduleType.FLEXIBLE_WEEKLY,
            targetCountPerPeriod = 3,
            startDate = LocalDate.of(2026, 10, 1)
        )
        val habitB = Habit(
            userId = "u1",
            title = "Habito B (Meta 3x)",
            scheduleType = ScheduleType.FLEXIBLE_WEEKLY,
            targetCountPerPeriod = 3,
            startDate = LocalDate.of(2026, 10, 1)
        )

        // Habito A: 4 execucoes na semana (4/3 -> Overachieved!)
        val logsA = (0..3).map { offset ->
            HabitExecutionLog(
                habitId = habitA.id,
                userId = "u1",
                executionDate = weekStart.plusDays(offset.toLong()),
                completedValue = 1.0,
                status = HabitLogStatus.COMPLETED
            )
        }
        // Habito B: 2 execucoes na semana (2/3)
        val logsB = (0..1).map { offset ->
            HabitExecutionLog(
                habitId = habitB.id,
                userId = "u1",
                executionDate = weekStart.plusDays(offset.toLong()),
                completedValue = 1.0,
                status = HabitLogStatus.COMPLETED
            )
        }

        val comparison = engine.buildExpectedVsAchievedComparison(
            periodType = PeriodType.WEEK,
            referenceDate = refDate,
            weekStartDay = DayOfWeek.MONDAY,
            habits = listOf(habitA, habitB),
            logs = logsA + logsB,
            skips = emptyList()
        )

        // Total esperado = 3 + 3 = 6
        // Total alcancado com trava = min(4,3) + min(2,3) = 3 + 2 = 5 -> 5/6 = 83.33%
        // Total alcancado bruto = 4 + 2 = 6
        comparison.overallMetrics.totalExpected shouldBe 6
        comparison.overallMetrics.totalAchievedCapped shouldBe 5
        comparison.overallMetrics.totalAchievedRaw shouldBe 6
        comparison.overallMetrics.consistencyPercentage shouldBe 83.33
        comparison.overallMetrics.overachievementCount shouldBe 1

        val breakdownA = comparison.habitBreakdown.first { it.habitId == habitA.id }
        breakdownA.expectedInPeriod shouldBe 3
        breakdownA.achievedRawInPeriod shouldBe 4
        breakdownA.achievedCappedInPeriod shouldBe 3
        breakdownA.percentageCapped shouldBe 100.0
        breakdownA.percentageRaw shouldBe 133.33
        breakdownA.statusBadge shouldBe HabitPeriodStatusBadge.OVERACHIEVED
    }

    @Test
    fun `deve reduzir o esperado ao registrar SKIPPED na semana`() {
        val refDate = LocalDate.of(2026, 10, 28)
        val weekStart = LocalDate.of(2026, 10, 26)

        val habit = Habit(
            userId = "u1",
            title = "Corrida 3x/semana",
            scheduleType = ScheduleType.FLEXIBLE_WEEKLY,
            targetCountPerPeriod = 3,
            startDate = LocalDate.of(2026, 10, 1)
        )

        val logs = listOf(
            HabitExecutionLog(habitId = habit.id, userId = "u1", executionDate = weekStart, completedValue = 1.0),
            HabitExecutionLog(habitId = habit.id, userId = "u1", executionDate = weekStart.plusDays(2), completedValue = 1.0)
        )
        val skips = listOf(
            HabitPeriodSkip(habitId = habit.id, userId = "u1", skipDate = weekStart.plusDays(4), skipCount = 1, reason = "Chuva forte")
        )

        val comparison = engine.buildExpectedVsAchievedComparison(
            periodType = PeriodType.WEEK,
            referenceDate = refDate,
            weekStartDay = DayOfWeek.MONDAY,
            habits = listOf(habit),
            logs = logs,
            skips = skips
        )

        comparison.overallMetrics.totalExpected shouldBe 2
        comparison.overallMetrics.totalAchievedCapped shouldBe 2
        comparison.overallMetrics.consistencyPercentage shouldBe 100.0
    }
}
