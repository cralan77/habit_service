package com.habit.domain.service

import com.habit.domain.model.Habit
import com.habit.domain.model.HabitPeriodSkip
import com.habit.domain.model.PeriodType
import com.habit.domain.model.ScheduleType
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class HabitScheduleEvaluatorServiceTest {

    private val service = HabitScheduleEvaluatorService()

    @Test
    fun `deve calcular janela semanal iniciando na Segunda-Feira (MONDAY)`() {
        val refDate = LocalDate.of(2026, 10, 28) // Wednesday
        val window = service.getWeekWindow(refDate, DayOfWeek.MONDAY)

        window.startDate shouldBe LocalDate.of(2026, 10, 26)
        window.endDate shouldBe LocalDate.of(2026, 11, 1)
        window.weekStartDay shouldBe DayOfWeek.MONDAY
    }

    @Test
    fun `deve calcular janela semanal iniciando no Domingo (SUNDAY)`() {
        val refDate = LocalDate.of(2026, 10, 28) // Wednesday
        val window = service.getWeekWindow(refDate, DayOfWeek.SUNDAY)

        window.startDate shouldBe LocalDate.of(2026, 10, 25)
        window.endDate shouldBe LocalDate.of(2026, 10, 31)
        window.weekStartDay shouldBe DayOfWeek.SUNDAY
    }

    @Test
    fun `deve ajustar dia 31 do mes para meses de 28, 29 e 30 dias`() {
        val febNonLeap = service.getEffectiveDaysOfMonth(setOf(15, 31), YearMonth.of(2026, 2))
        febNonLeap.shouldContainExactlyInAnyOrder(15, 28)

        val febLeap = service.getEffectiveDaysOfMonth(setOf(15, 31), YearMonth.of(2028, 2))
        febLeap.shouldContainExactlyInAnyOrder(15, 29)

        val april30 = service.getEffectiveDaysOfMonth(setOf(1, 31), YearMonth.of(2026, 4))
        april30.shouldContainExactlyInAnyOrder(1, 30)

        val oct31 = service.getEffectiveDaysOfMonth(setOf(1, 31), YearMonth.of(2026, 10))
        oct31.shouldContainExactlyInAnyOrder(1, 31)
    }

    @Test
    fun `deve avaliar corretamente se habito fixo e esperado na data`() {
        val startDate = LocalDate.of(2026, 10, 1)
        val weeklyHabit = Habit(
            userId = "user-1",
            title = "Academia Seg/Qua/Sex",
            scheduleType = ScheduleType.FIXED_WEEKLY,
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            startDate = startDate
        )

        service.isFixedHabitExpectedOnDate(weeklyHabit, LocalDate.of(2026, 10, 26)) shouldBe true  // Monday
        service.isFixedHabitExpectedOnDate(weeklyHabit, LocalDate.of(2026, 10, 27)) shouldBe false // Tuesday
        service.isFixedHabitExpectedOnDate(weeklyHabit, LocalDate.of(2026, 10, 28)) shouldBe true  // Wednesday
        service.isFixedHabitExpectedOnDate(weeklyHabit, LocalDate.of(2026, 9, 30)) shouldBe false  // Before startDate
    }

    @Test
    fun `deve descontar dias SKIPPED do esperado na semana`() {
        val habit = Habit(
            userId = "user-1",
            title = "Estudar Ingles",
            scheduleType = ScheduleType.DAILY,
            startDate = LocalDate.of(2026, 10, 1)
        )
        val weekStart = LocalDate.of(2026, 10, 26)
        val weekEnd = LocalDate.of(2026, 11, 1)
        val skips = listOf(
            HabitPeriodSkip(habitId = habit.id, userId = "user-1", skipDate = LocalDate.of(2026, 10, 27), skipCount = 1),
            HabitPeriodSkip(habitId = habit.id, userId = "user-1", skipDate = LocalDate.of(2026, 10, 29), skipCount = 1)
        )

        val expected = service.countExpectedInPeriod(
            habit = habit,
            periodType = PeriodType.WEEK,
            startDate = weekStart,
            endDate = weekEnd,
            skipsInRange = skips,
            weekStartDay = DayOfWeek.MONDAY
        )

        expected shouldBe 5
    }
}
