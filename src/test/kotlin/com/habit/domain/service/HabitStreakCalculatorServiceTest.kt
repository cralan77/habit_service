package com.habit.domain.service

import com.habit.domain.model.Habit
import com.habit.domain.model.HabitExecutionLog
import com.habit.domain.model.HabitPeriodSkip
import com.habit.domain.model.ScheduleType
import com.habit.domain.model.StreakUnit
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HabitStreakCalculatorServiceTest {

    private val scheduleEvaluator = HabitScheduleEvaluatorService()
    private val streakCalculator = HabitStreakCalculatorService(scheduleEvaluator)

    @Test
    fun `deve calcular streak de habito diario preservando dias SKIPPED`() {
        val startDate = LocalDate.of(2026, 10, 20)
        val refDate = LocalDate.of(2026, 10, 24)
        val habit = Habit(
            userId = "u1",
            title = "Leitura Diaria",
            scheduleType = ScheduleType.DAILY,
            startDate = startDate
        )

        // 20/10: Feito (1)
        // 21/10: Feito (2)
        // 22/10: SKIPPED (preserva 2)
        // 23/10: Feito (3)
        // 24/10 (hoje): Feito (4)
        val logs = listOf(
            HabitExecutionLog(habitId = habit.id, userId = "u1", executionDate = LocalDate.of(2026, 10, 20)),
            HabitExecutionLog(habitId = habit.id, userId = "u1", executionDate = LocalDate.of(2026, 10, 21)),
            HabitExecutionLog(habitId = habit.id, userId = "u1", executionDate = LocalDate.of(2026, 10, 23)),
            HabitExecutionLog(habitId = habit.id, userId = "u1", executionDate = LocalDate.of(2026, 10, 24))
        )
        val skips = listOf(
            HabitPeriodSkip(habitId = habit.id, userId = "u1", skipDate = LocalDate.of(2026, 10, 22), reason = "Viagem")
        )

        val result = streakCalculator.calculateStreak(habit, logs, skips, refDate)

        result.currentStreak shouldBe 4
        result.bestStreak shouldBe 4
        result.streakUnit shouldBe StreakUnit.DAYS
    }

    @Test
    fun `deve calcular streak de habito semanal fixo ignorando dias nao agendados`() {
        // Seg (19/10), Qua (21/10), Sex (23/10)
        val habit = Habit(
            userId = "u1",
            title = "Musculacao Seg/Qua/Sex",
            scheduleType = ScheduleType.FIXED_WEEKLY,
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            startDate = LocalDate.of(2026, 10, 19)
        )

        val logs = listOf(
            HabitExecutionLog(habitId = habit.id, userId = "u1", executionDate = LocalDate.of(2026, 10, 19)),
            HabitExecutionLog(habitId = habit.id, userId = "u1", executionDate = LocalDate.of(2026, 10, 21)),
            HabitExecutionLog(habitId = habit.id, userId = "u1", executionDate = LocalDate.of(2026, 10, 23))
        )

        val result = streakCalculator.calculateStreak(
            habit = habit,
            logs = logs,
            skips = emptyList(),
            referenceDate = LocalDate.of(2026, 10, 24) // Saturday
        )

        result.currentStreak shouldBe 3
        result.bestStreak shouldBe 3
        result.streakUnit shouldBe StreakUnit.DAYS
    }

    @Test
    fun `deve calcular streak em semanas consecutivas para habito FLEXIBLE_WEEKLY`() {
        // Semana 1: 12/10 a 18/10 (3 execucoes) -> Sucesso (Streak = 1)
        // Semana 2: 19/10 a 25/10 (3 execucoes) -> Sucesso (Streak = 2)
        // Semana 3 (atual): 26/10 a 01/11 (1 execucao ate quarta 28/10) -> Preserva Streak = 2
        val habit = Habit(
            userId = "u1",
            title = "Academia 3x/semana",
            scheduleType = ScheduleType.FLEXIBLE_WEEKLY,
            targetCountPerPeriod = 3,
            startDate = LocalDate.of(2026, 10, 12)
        )

        val logs = listOf(
            // Semana 1
            HabitExecutionLog(habitId = habit.id, userId = "u1", executionDate = LocalDate.of(2026, 10, 13)),
            HabitExecutionLog(habitId = habit.id, userId = "u1", executionDate = LocalDate.of(2026, 10, 15)),
            HabitExecutionLog(habitId = habit.id, userId = "u1", executionDate = LocalDate.of(2026, 10, 17)),
            // Semana 2
            HabitExecutionLog(habitId = habit.id, userId = "u1", executionDate = LocalDate.of(2026, 10, 19)),
            HabitExecutionLog(habitId = habit.id, userId = "u1", executionDate = LocalDate.of(2026, 10, 20)),
            HabitExecutionLog(habitId = habit.id, userId = "u1", executionDate = LocalDate.of(2026, 10, 24)),
            // Semana 3 (em andamento)
            HabitExecutionLog(habitId = habit.id, userId = "u1", executionDate = LocalDate.of(2026, 10, 26))
        )

        val result = streakCalculator.calculateStreak(
            habit = habit,
            logs = logs,
            skips = emptyList(),
            referenceDate = LocalDate.of(2026, 10, 28),
            weekStartDay = DayOfWeek.MONDAY
        )

        result.currentStreak shouldBe 2
        result.bestStreak shouldBe 2
        result.streakUnit shouldBe StreakUnit.WEEKS
    }
}
