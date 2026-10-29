package com.habit.domain.service

import com.habit.domain.model.CompletionMode
import com.habit.domain.model.Habit
import com.habit.domain.model.HabitExecutionLog
import com.habit.domain.model.HabitLogStatus
import com.habit.domain.model.HabitPeriodSkip
import com.habit.domain.model.HealthTriggerType
import com.habit.domain.model.PeriodType
import com.habit.domain.model.QuotaCountMode
import com.habit.domain.model.ScheduleType
import com.habit.domain.model.WeekWindow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class HabitScheduleEvaluatorService {

    /**
     * Calcula a janela de 7 dias da semana que contém [date], iniciando em [weekStartDay] (padrão MONDAY).
     */
    fun getWeekWindow(date: LocalDate, weekStartDay: DayOfWeek = DayOfWeek.MONDAY): WeekWindow {
        val diff = (date.dayOfWeek.value - weekStartDay.value + 7) % 7
        val start = date.minusDays(diff.toLong())
        val end = start.plusDays(6)
        return WeekWindow(
            startDate = start,
            endDate = end,
            weekStartDay = weekStartDay
        )
    }

    /**
     * Calcula a janela do mês calendário que contém [date].
     */
    fun getMonthWindow(date: LocalDate): Pair<LocalDate, LocalDate> {
        val ym = YearMonth.from(date)
        return ym.atDay(1) to ym.atEndOfMonth()
    }

    /**
     * Ajusta dias fixos do mês (ex: 31) para o último dia real do mês (28, 29 ou 30).
     */
    fun getEffectiveDaysOfMonth(daysOfMonth: Set<Int>, yearMonth: YearMonth): Set<Int> {
        if (daysOfMonth.isEmpty()) return emptySet()
        val maxDay = yearMonth.lengthOfMonth()
        return daysOfMonth.map { day -> min(max(1, day), maxDay) }.toSet()
    }

    /**
     * Verifica se um hábito de agendamento FIXO (DAILY, FIXED_WEEKLY, FIXED_MONTHLY) é esperado na data [date].
     * Hábitos flexíveis retornam false aqui pois pertencem ao pool de cota da semana/mês.
     */
    fun isFixedHabitExpectedOnDate(habit: Habit, date: LocalDate): Boolean {
        if (!habit.isActive || date.isBefore(habit.startDate)) return false
        return when (habit.scheduleType) {
            ScheduleType.DAILY -> true
            ScheduleType.FIXED_WEEKLY -> habit.daysOfWeek.contains(date.dayOfWeek)
            ScheduleType.FIXED_MONTHLY -> {
                val effectiveDays = getEffectiveDaysOfMonth(habit.daysOfMonth, YearMonth.from(date))
                effectiveDays.contains(date.dayOfMonth)
            }
            ScheduleType.FLEXIBLE_WEEKLY,
            ScheduleType.FLEXIBLE_MONTHLY -> false
        }
    }

    /**
     * Soma o valor concluído no dia para um hábito.
     */
    fun getDailyValue(habit: Habit, logsOnDate: List<HabitExecutionLog>): Double {
        if (logsOnDate.isEmpty()) return 0.0
        return when {
            habit.completionMode == CompletionMode.QUANTITATIVE ->
                logsOnDate.sumOf { it.completedValue }
            habit.completionMode == CompletionMode.HEALTH_AUTO && habit.healthTriggerType == HealthTriggerType.DAILY_STEPS ->
                logsOnDate.maxOfOrNull { it.completedValue } ?: 0.0
            else ->
                logsOnDate.count { it.status == HabitLogStatus.COMPLETED }.toDouble()
        }
    }

    /**
     * Calcula o progresso percentual (0.0 a 1.0) de um hábito em um dia específico.
     */
    fun getDailyProgressRatio(habit: Habit, logsOnDate: List<HabitExecutionLog>): Double {
        if (logsOnDate.isEmpty()) return 0.0
        return when {
            habit.completionMode == CompletionMode.QUANTITATIVE ||
                (habit.completionMode == CompletionMode.HEALTH_AUTO && habit.healthTriggerType == HealthTriggerType.DAILY_STEPS) -> {
                val target = if (habit.dailyTargetValue > 0.0) habit.dailyTargetValue else 1.0
                min(1.0, getDailyValue(habit, logsOnDate) / target)
            }
            else -> {
                val completed = logsOnDate.any { it.status == HabitLogStatus.COMPLETED && it.completedValue >= 1.0 }
                if (completed) 1.0 else 0.0
            }
        }
    }

    fun isHabitCompletedOnDate(habit: Habit, logsOnDate: List<HabitExecutionLog>): Boolean {
        return getDailyProgressRatio(habit, logsOnDate) >= 1.0
    }

    /**
     * Conta quantas unidades/dias o hábito alcançou dentro do intervalo [startDate, endDate].
     * Respeita QuotaCountMode (DAYS_COMPLETED vs TOTAL_EXECUTIONS).
     */
    fun countAchievedInRange(
        habit: Habit,
        logsInRange: List<HabitExecutionLog>,
        startDate: LocalDate,
        endDate: LocalDate
    ): Int {
        val validLogs = logsInRange.filter { !it.executionDate.isBefore(startDate) && !it.executionDate.isAfter(endDate) }
        if (validLogs.isEmpty()) return 0

        val useTotalExecutions = habit.quotaCountMode == QuotaCountMode.TOTAL_EXECUTIONS ||
            habit.completionMode == CompletionMode.TODO_LINKED

        return if (useTotalExecutions) {
            validLogs.count { it.status == HabitLogStatus.COMPLETED }
        } else {
            validLogs.groupBy { it.executionDate }
                .count { (_, dayLogs) -> isHabitCompletedOnDate(habit, dayLogs) }
        }
    }

    /**
     * Calcula a quantidade esperada de execuções de um hábito em um período (WEEK ou MONTH),
     * já descontando registros de pulo/congelamento (SKIPPED).
     */
    fun countExpectedInPeriod(
        habit: Habit,
        periodType: PeriodType,
        startDate: LocalDate,
        endDate: LocalDate,
        skipsInRange: List<HabitPeriodSkip>,
        weekStartDay: DayOfWeek = DayOfWeek.MONDAY
    ): Int {
        if (!habit.isActive || endDate.isBefore(habit.startDate)) return 0
        val effectiveStart = if (habit.startDate.isAfter(startDate)) habit.startDate else startDate

        val validSkips = skipsInRange.filter {
            !it.skipDate.isBefore(effectiveStart) && !it.skipDate.isAfter(endDate)
        }

        return when (habit.scheduleType) {
            ScheduleType.DAILY,
            ScheduleType.FIXED_WEEKLY,
            ScheduleType.FIXED_MONTHLY -> {
                var rawExpected = 0
                var skippedExpectedDays = 0
                val skipDates = validSkips.map { it.skipDate }.toSet()
                var cursor = effectiveStart
                while (!cursor.isAfter(endDate)) {
                    if (isFixedHabitExpectedOnDate(habit, cursor)) {
                        rawExpected++
                        if (skipDates.contains(cursor)) {
                            skippedExpectedDays++
                        }
                    }
                    cursor = cursor.plusDays(1)
                }
                max(0, rawExpected - skippedExpectedDays)
            }

            ScheduleType.FLEXIBLE_WEEKLY -> {
                val totalSkips = validSkips.sumOf { it.skipCount }
                if (periodType == PeriodType.WEEK) {
                    max(0, habit.targetCountPerPeriod - totalSkips)
                } else {
                    // Em visão mensal, conta as janelas semanais cujo início (ou interseção ativa) pertence ao mês
                    var weeksCount = 0
                    var cursor = getWeekWindow(effectiveStart, weekStartDay).startDate
                    while (!cursor.isAfter(endDate)) {
                        val weekEnd = cursor.plusDays(6)
                        val overlapStart = if (cursor.isBefore(effectiveStart)) effectiveStart else cursor
                        val overlapEnd = if (weekEnd.isAfter(endDate)) endDate else weekEnd
                        val overlapDays = java.time.temporal.ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1
                        if (overlapDays >= 4) {
                            weeksCount++
                        }
                        cursor = cursor.plusDays(7)
                    }
                    val effectiveWeeks = max(1, weeksCount)
                    max(0, effectiveWeeks * habit.targetCountPerPeriod - totalSkips)
                }
            }

            ScheduleType.FLEXIBLE_MONTHLY -> {
                val totalSkips = validSkips.sumOf { it.skipCount }
                if (periodType == PeriodType.MONTH) {
                    max(0, habit.targetCountPerPeriod - totalSkips)
                } else {
                    // Em visão semanal, hábito mensal flexível não tem obrigação fixa semanal (fica na meta mensal),
                    // mas se for avaliado isoladamente no período mensal retorna targetCountPerPeriod.
                    0
                }
            }
        }
    }

    fun formatPeriodLabel(habit: Habit, referenceDate: LocalDate, weekStartDay: DayOfWeek): String {
        return when (habit.scheduleType) {
            ScheduleType.FLEXIBLE_MONTHLY, ScheduleType.FIXED_MONTHLY -> {
                val monthName = referenceDate.month.getDisplayName(TextStyle.FULL, Locale.of("pt", "BR"))
                    .replaceFirstChar { it.uppercase() }
                "$monthName/${referenceDate.year}"
            }
            else -> {
                val window = getWeekWindow(referenceDate, weekStartDay)
                "Semana ${window.startDate.dayOfMonth.toString().padStart(2, '0')}/${window.startDate.monthValue.toString().padStart(2, '0')}"
            }
        }
    }
}
