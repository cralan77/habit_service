package com.habit.domain.service

import com.habit.domain.model.BarChartPoint
import com.habit.domain.model.DailyGauge
import com.habit.domain.model.DailyRoutineView
import com.habit.domain.model.ExpectedVsAchievedComparison
import com.habit.domain.model.FixedHabitTodayItem
import com.habit.domain.model.FlexibleHabitInPeriodItem
import com.habit.domain.model.Habit
import com.habit.domain.model.HabitBreakdownItem
import com.habit.domain.model.HabitExecutionLog
import com.habit.domain.model.HabitHeatmapResult
import com.habit.domain.model.HabitPeriodSkip
import com.habit.domain.model.HabitPeriodStatusBadge
import com.habit.domain.model.HabitStatusToday
import com.habit.domain.model.HeatmapDayCell
import com.habit.domain.model.OverallComparisonMetrics
import com.habit.domain.model.PeriodType
import com.habit.domain.model.ScheduleType
import com.habit.domain.model.TodoCategory
import com.habit.domain.model.TodoItem
import com.habit.domain.model.TodoPriority
import com.habit.domain.model.TodoStatus
import com.habit.domain.model.TrendLinePoint
import com.habit.domain.model.UrgentTodoSummaryItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class HabitAnalyticsEngineService(
    private val scheduleEvaluator: HabitScheduleEvaluatorService,
    private val streakCalculator: HabitStreakCalculatorService
) {

    /**
     * Constrói a Visão Diária (Hoje) separando:
     * 1. Hábitos Fixos do Dia (DAILY, FIXED_WEEKLY agendados hoje, FIXED_MONTHLY agendados hoje)
     * 2. Hábitos Flexíveis do Período (FLEXIBLE_WEEKLY e FLEXIBLE_MONTHLY) com bônus quando executados hoje
     * 3. Tarefas Urgentes / Vencendo Hoje ou Atrasadas
     */
    fun buildDailyRoutineView(
        date: LocalDate,
        weekStartDay: DayOfWeek,
        habits: List<Habit>,
        logs: List<HabitExecutionLog>,
        skips: List<HabitPeriodSkip>,
        todos: List<TodoItem>,
        categoriesById: Map<UUID, TodoCategory>
    ): DailyRoutineView {
        val activeHabits = habits.filter { it.isActive && !date.isBefore(it.startDate) }
        val weekWindow = scheduleEvaluator.getWeekWindow(date, weekStartDay)
        val monthWindow = scheduleEvaluator.getMonthWindow(date)

        val logsByHabit = logs.groupBy { it.habitId }
        val skipsByHabit = skips.groupBy { it.habitId }

        val fixedHabitsToday = mutableListOf<FixedHabitTodayItem>()
        val flexibleHabitsInPeriod = mutableListOf<FlexibleHabitInPeriodItem>()

        var fixedExpectedCount = 0
        var fixedCompletedCount = 0
        var fixedSkippedCount = 0
        var flexibleCompletedTodayBonus = 0

        for (habit in activeHabits) {
            val habitLogs = logsByHabit[habit.id].orEmpty()
            val habitSkips = skipsByHabit[habit.id].orEmpty()
            val logsToday = habitLogs.filter { it.executionDate.isEqual(date) }
            val skippedToday = habitSkips.any { it.skipDate.isEqual(date) }
            val streak = streakCalculator.calculateStreak(habit, habitLogs, habitSkips, date, weekStartDay)

            if (habit.isFixedSchedule()) {
                if (scheduleEvaluator.isFixedHabitExpectedOnDate(habit, date)) {
                    val valueToday = scheduleEvaluator.getDailyValue(habit, logsToday)
                    val ratioToday = scheduleEvaluator.getDailyProgressRatio(habit, logsToday)
                    val isCompletedToday = ratioToday >= 1.0

                    val statusToday = when {
                        isCompletedToday -> HabitStatusToday.COMPLETED
                        skippedToday -> HabitStatusToday.SKIPPED
                        valueToday > 0.0 -> HabitStatusToday.PARTIAL
                        else -> HabitStatusToday.PENDING
                    }

                    if (skippedToday && !isCompletedToday) {
                        fixedSkippedCount++
                    } else {
                        fixedExpectedCount++
                        if (isCompletedToday) {
                            fixedCompletedCount++
                        }
                    }

                    fixedHabitsToday.add(
                        FixedHabitTodayItem(
                            habitId = habit.id,
                            title = habit.title,
                            description = habit.description,
                            icon = habit.icon,
                            colorHex = habit.colorHex,
                            scheduleType = habit.scheduleType,
                            completionMode = habit.completionMode,
                            dailyTargetValue = habit.dailyTargetValue,
                            currentValueToday = valueToday,
                            unit = habit.unit,
                            progressPercentage = roundOneDecimal(ratioToday * 100.0),
                            statusToday = statusToday,
                            currentStreak = streak.currentStreak,
                            bestStreak = streak.bestStreak,
                            streakUnit = streak.streakUnit
                        )
                    )
                }
            } else {
                // Hábito Flexível (Semanal ou Mensal)
                val (periodStart, periodEnd) = if (habit.scheduleType == ScheduleType.FLEXIBLE_WEEKLY) {
                    weekWindow.startDate to weekWindow.endDate
                } else {
                    monthWindow.first to monthWindow.second
                }
                val pType = if (habit.scheduleType == ScheduleType.FLEXIBLE_WEEKLY) PeriodType.WEEK else PeriodType.MONTH

                val skipsInPeriod = habitSkips.filter {
                    !it.skipDate.isBefore(periodStart) && !it.skipDate.isAfter(periodEnd)
                }
                val periodTarget = scheduleEvaluator.countExpectedInPeriod(
                    habit = habit,
                    periodType = pType,
                    startDate = periodStart,
                    endDate = periodEnd,
                    skipsInRange = skipsInPeriod,
                    weekStartDay = weekStartDay
                )
                val periodAchieved = scheduleEvaluator.countAchievedInRange(
                    habit = habit,
                    logsInRange = habitLogs,
                    startDate = periodStart,
                    endDate = periodEnd
                )
                val executedToday = scheduleEvaluator.isHabitCompletedOnDate(habit, logsToday)
                if (executedToday) {
                    flexibleCompletedTodayBonus++
                }

                flexibleHabitsInPeriod.add(
                    FlexibleHabitInPeriodItem(
                        habitId = habit.id,
                        title = habit.title,
                        description = habit.description,
                        icon = habit.icon,
                        colorHex = habit.colorHex,
                        scheduleType = habit.scheduleType,
                        completionMode = habit.completionMode,
                        linkedTodoCategoryId = habit.linkedTodoCategoryId,
                        linkedTodoCategoryName = habit.linkedTodoCategoryId?.let { categoriesById[it]?.name },
                        periodLabel = scheduleEvaluator.formatPeriodLabel(habit, date, weekStartDay),
                        periodTargetCount = periodTarget,
                        periodAchievedCount = periodAchieved,
                        periodSkippedCount = skipsInPeriod.sumOf { it.skipCount },
                        isPeriodGoalMet = periodAchieved >= periodTarget && periodTarget > 0,
                        isOverachieved = periodAchieved > periodTarget && periodTarget > 0,
                        executedToday = executedToday,
                        currentStreak = streak.currentStreak,
                        bestStreak = streak.bestStreak,
                        streakUnit = streak.streakUnit
                    )
                )
            }
        }

        val fixedSuccessPercentage = if (fixedExpectedCount == 0) {
            100.0
        } else {
            roundOneDecimal((fixedCompletedCount.toDouble() / fixedExpectedCount.toDouble()) * 100.0)
        }

        val habitsById = habits.associateBy { it.id }
        val urgentTodos = todos
            .filter { todo ->
                todo.status == TodoStatus.PENDING &&
                    (todo.dueDate != null && !todo.dueDate.isAfter(date) ||
                        todo.priority == TodoPriority.URGENT ||
                        todo.priority == TodoPriority.HIGH)
            }
            .sortedWith(
                compareBy<TodoItem> { it.dueDate ?: LocalDate.MAX }
                    .thenByDescending { it.priority.ordinal }
            )
            .map { todo ->
                val effectiveTitles = linkedSetOf<String>()
                todo.linkedHabitId?.let { hId -> habitsById[hId]?.title?.let { effectiveTitles.add(it) } }
                todo.categoryId?.let { catId ->
                    activeHabits.filter { it.linkedTodoCategoryId == catId }.forEach { effectiveTitles.add(it.title) }
                }
                UrgentTodoSummaryItem(
                    id = todo.id,
                    title = todo.title,
                    categoryId = todo.categoryId,
                    categoryName = todo.categoryId?.let { categoriesById[it]?.name },
                    linkedHabitId = todo.linkedHabitId,
                    effectiveLinkedHabitTitles = effectiveTitles.toList(),
                    priority = todo.priority,
                    status = todo.status,
                    dueDate = todo.dueDate,
                    subtasksCompleted = todo.subtasks.count { it.isCompleted },
                    subtasksTotal = todo.subtasks.size
                )
            }

        return DailyRoutineView(
            date = date,
            dayOfWeek = date.dayOfWeek,
            weekWindow = weekWindow,
            dailyGauge = DailyGauge(
                fixedExpectedCount = fixedExpectedCount,
                fixedCompletedCount = fixedCompletedCount,
                fixedSkippedCount = fixedSkippedCount,
                fixedSuccessPercentage = fixedSuccessPercentage,
                flexibleCompletedTodayBonus = flexibleCompletedTodayBonus
            ),
            fixedHabitsToday = fixedHabitsToday,
            flexibleHabitsInPeriod = flexibleHabitsInPeriod,
            urgentAndTodayTodos = urgentTodos
        )
    }

    /**
     * Constrói o comparativo gráfico "Esperado vs. Alcançado" para Semana (WEEK) ou Mês (MONTH).
     * Aplica o Cap de 100% por hábito na taxa geral (Decisão D-07) e preserva o valor bruto de superação (Overachievement).
     */
    fun buildExpectedVsAchievedComparison(
        periodType: PeriodType,
        referenceDate: LocalDate,
        weekStartDay: DayOfWeek,
        habits: List<Habit>,
        logs: List<HabitExecutionLog>,
        skips: List<HabitPeriodSkip>
    ): ExpectedVsAchievedComparison {
        val (startDate, endDate) = if (periodType == PeriodType.WEEK) {
            val w = scheduleEvaluator.getWeekWindow(referenceDate, weekStartDay)
            w.startDate to w.endDate
        } else {
            scheduleEvaluator.getMonthWindow(referenceDate)
        }

        val activeHabits = habits.filter { it.isActive && !endDate.isBefore(it.startDate) }
        val logsByHabit = logs.groupBy { it.habitId }
        val skipsByHabit = skips.groupBy { it.habitId }

        val breakdown = mutableListOf<HabitBreakdownItem>()
        var totalExpected = 0
        var totalAchievedCapped = 0
        var totalAchievedRaw = 0
        var totalSkipped = 0
        var overachievementCount = 0

        for (habit in activeHabits) {
            val habitLogs = logsByHabit[habit.id].orEmpty()
            val habitSkips = skipsByHabit[habit.id].orEmpty().filter {
                !it.skipDate.isBefore(startDate) && !it.skipDate.isAfter(endDate)
            }

            // Em visão semanal, se um hábito é FLEXIBLE_MONTHLY e teve execuções na semana,
            // ou se estamos na visão mensal:
            val achievedRaw = scheduleEvaluator.countAchievedInRange(habit, habitLogs, startDate, endDate)
            val expected = if (periodType == PeriodType.WEEK && habit.scheduleType == ScheduleType.FLEXIBLE_MONTHLY) {
                // Na aba semanal, hábito mensal flexível não penaliza o esperado semanal se não foi executado,
                // mas se foi executado na semana, entra como bônus/meta até o limite da cota mensal.
                min(habit.targetCountPerPeriod, achievedRaw)
            } else {
                scheduleEvaluator.countExpectedInPeriod(
                    habit = habit,
                    periodType = periodType,
                    startDate = startDate,
                    endDate = endDate,
                    skipsInRange = habitSkips,
                    weekStartDay = weekStartDay
                )
            }

            // Se é FLEXIBLE_MONTHLY na visão semanal e tem expected == 0 && achievedRaw == 0, não polui a soma semanal
            if (periodType == PeriodType.WEEK && habit.scheduleType == ScheduleType.FLEXIBLE_MONTHLY && expected == 0 && achievedRaw == 0) {
                continue
            }

            val achievedCapped = min(achievedRaw, expected)
            val skippedCount = habitSkips.sumOf { it.skipCount }

            totalExpected += expected
            totalAchievedCapped += achievedCapped
            totalAchievedRaw += achievedRaw
            totalSkipped += skippedCount

            if (achievedRaw > expected && expected > 0) {
                overachievementCount += (achievedRaw - expected)
            }

            val pctCapped = if (expected == 0) 100.0 else roundTwoDecimals((achievedCapped.toDouble() / expected.toDouble()) * 100.0)
            val pctRaw = if (expected == 0) {
                if (achievedRaw > 0) 100.0 else 100.0
            } else {
                roundTwoDecimals((achievedRaw.toDouble() / expected.toDouble()) * 100.0)
            }

            val badge = when {
                expected == 0 && skippedCount > 0 -> HabitPeriodStatusBadge.SKIPPED
                achievedRaw > expected && expected > 0 -> HabitPeriodStatusBadge.OVERACHIEVED
                achievedRaw >= expected && expected > 0 -> HabitPeriodStatusBadge.COMPLETED
                achievedRaw > 0 -> HabitPeriodStatusBadge.ON_TRACK
                else -> HabitPeriodStatusBadge.BEHIND
            }

            breakdown.add(
                HabitBreakdownItem(
                    habitId = habit.id,
                    title = habit.title,
                    icon = habit.icon,
                    colorHex = habit.colorHex,
                    scheduleType = habit.scheduleType,
                    completionMode = habit.completionMode,
                    expectedInPeriod = expected,
                    achievedRawInPeriod = achievedRaw,
                    achievedCappedInPeriod = achievedCapped,
                    skippedInPeriod = skippedCount,
                    percentageCapped = pctCapped,
                    percentageRaw = pctRaw,
                    statusBadge = badge
                )
            )
        }

        val overallConsistency = if (totalExpected == 0) {
            100.0
        } else {
            roundTwoDecimals((totalAchievedCapped.toDouble() / totalExpected.toDouble()) * 100.0)
        }

        val barChartSeries = buildBarChartSeries(
            periodType = periodType,
            startDate = startDate,
            endDate = endDate,
            activeHabits = activeHabits,
            logsByHabit = logsByHabit,
            skipsByHabit = skipsByHabit
        )

        val trendLineSeries = buildTrendLineSeries(
            periodType = periodType,
            referenceDate = referenceDate,
            weekStartDay = weekStartDay,
            habits = habits,
            logs = logs,
            skips = skips
        )

        return ExpectedVsAchievedComparison(
            periodType = periodType,
            startDate = startDate,
            endDate = endDate,
            weekStartDay = weekStartDay,
            overallMetrics = OverallComparisonMetrics(
                totalExpected = totalExpected,
                totalAchievedCapped = totalAchievedCapped,
                totalAchievedRaw = totalAchievedRaw,
                totalSkipped = totalSkipped,
                consistencyPercentage = overallConsistency,
                overachievementCount = overachievementCount
            ),
            barChartSeries = barChartSeries,
            habitBreakdown = breakdown,
            trendLineSeries = trendLineSeries
        )
    }

    private fun buildBarChartSeries(
        periodType: PeriodType,
        startDate: LocalDate,
        endDate: LocalDate,
        activeHabits: List<Habit>,
        logsByHabit: Map<UUID, List<HabitExecutionLog>>,
        skipsByHabit: Map<UUID, List<HabitPeriodSkip>>
    ): List<BarChartPoint> {
        val points = mutableListOf<BarChartPoint>()
        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            var fixedExp = 0
            var fixedAch = 0
            var flexExec = 0

            for (habit in activeHabits) {
                val logsToday = logsByHabit[habit.id].orEmpty().filter { it.executionDate.isEqual(cursor) }
                val skippedToday = skipsByHabit[habit.id].orEmpty().any { it.skipDate.isEqual(cursor) }

                if (habit.isFixedSchedule()) {
                    if (scheduleEvaluator.isFixedHabitExpectedOnDate(habit, cursor)) {
                        val completed = scheduleEvaluator.isHabitCompletedOnDate(habit, logsToday)
                        if (!skippedToday || completed) {
                            fixedExp++
                            if (completed) fixedAch++
                        }
                    }
                } else {
                    val countToday = scheduleEvaluator.countAchievedInRange(habit, logsToday, cursor, cursor)
                    if (countToday > 0) {
                        flexExec += countToday
                    }
                }
            }

            val totalExpBar = fixedExp + flexExec
            val totalAchBar = fixedAch + flexExec
            val pct = if (totalExpBar == 0) 100.0 else roundOneDecimal((totalAchBar.toDouble() / totalExpBar.toDouble()) * 100.0)

            val dayShort = cursor.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.of("pt", "BR"))
                .replace(".", "")
                .replaceFirstChar { it.uppercase() }
            val label = if (periodType == PeriodType.WEEK) {
                "$dayShort (${cursor.dayOfMonth.toString().padStart(2, '0')}/${cursor.monthValue.toString().padStart(2, '0')})"
            } else {
                "${cursor.dayOfMonth.toString().padStart(2, '0')}/${cursor.monthValue.toString().padStart(2, '0')}"
            }

            points.add(
                BarChartPoint(
                    label = label,
                    date = cursor,
                    fixedExpected = fixedExp,
                    fixedAchieved = fixedAch,
                    flexibleExecutedToday = flexExec,
                    totalExpectedForBar = totalExpBar,
                    totalAchievedForBar = totalAchBar,
                    percentage = pct
                )
            )
            cursor = cursor.plusDays(1)
        }
        return points
    }

    private fun buildTrendLineSeries(
        periodType: PeriodType,
        referenceDate: LocalDate,
        weekStartDay: DayOfWeek,
        habits: List<Habit>,
        logs: List<HabitExecutionLog>,
        skips: List<HabitPeriodSkip>
    ): List<TrendLinePoint> {
        val points = mutableListOf<TrendLinePoint>()
        val logsByHabit = logs.groupBy { it.habitId }
        val skipsByHabit = skips.groupBy { it.habitId }

        if (periodType == PeriodType.WEEK) {
            val currentWindow = scheduleEvaluator.getWeekWindow(referenceDate, weekStartDay)
            for (i in 5 downTo 0) {
                val wStart = currentWindow.startDate.minusWeeks(i.toLong())
                val wEnd = wStart.plusDays(6)
                val pct = computeSimplePeriodConsistency(
                    periodType = PeriodType.WEEK,
                    startDate = wStart,
                    endDate = wEnd,
                    weekStartDay = weekStartDay,
                    habits = habits,
                    logsByHabit = logsByHabit,
                    skipsByHabit = skipsByHabit
                )
                points.add(
                    TrendLinePoint(
                        periodLabel = "Sem ${wStart.dayOfMonth.toString().padStart(2, '0')}/${wStart.monthValue.toString().padStart(2, '0')}",
                        startDate = wStart,
                        consistencyPercentage = pct
                    )
                )
            }
        } else {
            val currentYm = java.time.YearMonth.from(referenceDate)
            for (i in 5 downTo 0) {
                val ym = currentYm.minusMonths(i.toLong())
                val mStart = ym.atDay(1)
                val mEnd = ym.atEndOfMonth()
                val pct = computeSimplePeriodConsistency(
                    periodType = PeriodType.MONTH,
                    startDate = mStart,
                    endDate = mEnd,
                    weekStartDay = weekStartDay,
                    habits = habits,
                    logsByHabit = logsByHabit,
                    skipsByHabit = skipsByHabit
                )
                val mLabel = ym.month.getDisplayName(TextStyle.SHORT, Locale.of("pt", "BR"))
                    .replace(".", "")
                    .replaceFirstChar { it.uppercase() }
                points.add(
                    TrendLinePoint(
                        periodLabel = "$mLabel/${ym.year % 100}",
                        startDate = mStart,
                        consistencyPercentage = pct
                    )
                )
            }
        }
        return points
    }

    private fun computeSimplePeriodConsistency(
        periodType: PeriodType,
        startDate: LocalDate,
        endDate: LocalDate,
        weekStartDay: DayOfWeek,
        habits: List<Habit>,
        logsByHabit: Map<UUID, List<HabitExecutionLog>>,
        skipsByHabit: Map<UUID, List<HabitPeriodSkip>>
    ): Double {
        val activeHabits = habits.filter { it.isActive && !endDate.isBefore(it.startDate) }
        if (activeHabits.isEmpty()) return 0.0
        var totalExp = 0
        var totalAchCapped = 0

        for (habit in activeHabits) {
            val hLogs = logsByHabit[habit.id].orEmpty()
            val hSkips = skipsByHabit[habit.id].orEmpty().filter {
                !it.skipDate.isBefore(startDate) && !it.skipDate.isAfter(endDate)
            }
            val achRaw = scheduleEvaluator.countAchievedInRange(habit, hLogs, startDate, endDate)
            val exp = if (periodType == PeriodType.WEEK && habit.scheduleType == ScheduleType.FLEXIBLE_MONTHLY) {
                min(habit.targetCountPerPeriod, achRaw)
            } else {
                scheduleEvaluator.countExpectedInPeriod(habit, periodType, startDate, endDate, hSkips, weekStartDay)
            }
            totalExp += exp
            totalAchCapped += min(achRaw, exp)
        }
        return if (totalExp == 0) 0.0 else roundOneDecimal((totalAchCapped.toDouble() / totalExp.toDouble()) * 100.0)
    }

    /**
     * Constrói o Heatmap de Consistência (intensidade 0..4 por dia) para os últimos [months] meses.
     */
    fun buildHeatmap(
        months: Int,
        referenceDate: LocalDate,
        habitId: UUID?,
        habits: List<Habit>,
        logs: List<HabitExecutionLog>,
        skips: List<HabitPeriodSkip>
    ): HabitHeatmapResult {
        val clampedMonths = months.coerceIn(1, 12)
        val startDate = referenceDate.minusMonths(clampedMonths.toLong()).withDayOfMonth(1)
        val endDate = referenceDate

        val filteredHabits = if (habitId != null) {
            habits.filter { it.id == habitId }
        } else {
            habits.filter { it.isActive }
        }

        val logsByHabit = logs.groupBy { it.habitId }
        val skipsByHabit = skips.groupBy { it.habitId }

        val cells = mutableListOf<HeatmapDayCell>()
        var totalActiveDays = 0

        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            var expectedOnDay = 0
            var completedOnDay = 0

            for (habit in filteredHabits) {
                if (cursor.isBefore(habit.startDate)) continue
                val logsToday = logsByHabit[habit.id].orEmpty().filter { it.executionDate.isEqual(cursor) }
                val skippedToday = skipsByHabit[habit.id].orEmpty().any { it.skipDate.isEqual(cursor) }
                val isCompleted = scheduleEvaluator.isHabitCompletedOnDate(habit, logsToday)

                if (habit.isFixedSchedule()) {
                    if (scheduleEvaluator.isFixedHabitExpectedOnDate(habit, cursor) && (!skippedToday || isCompleted)) {
                        expectedOnDay++
                        if (isCompleted) completedOnDay++
                    }
                } else {
                    if (isCompleted) {
                        expectedOnDay++
                        completedOnDay++
                    }
                }
            }

            if (completedOnDay > 0) {
                totalActiveDays++
            }

            val pct = when {
                expectedOnDay == 0 && completedOnDay == 0 -> 0.0
                expectedOnDay == 0 -> 100.0
                else -> roundOneDecimal((completedOnDay.toDouble() / expectedOnDay.toDouble()) * 100.0)
            }

            val intensity = when {
                completedOnDay == 0 -> 0
                pct >= 100.0 -> 4
                pct >= 75.0 -> 3
                pct >= 40.0 -> 2
                else -> 1
            }

            cells.add(
                HeatmapDayCell(
                    date = cursor,
                    dayOfWeek = cursor.dayOfWeek,
                    completedCount = completedOnDay,
                    expectedCount = expectedOnDay,
                    intensityLevel = intensity,
                    percentage = pct
                )
            )
            cursor = cursor.plusDays(1)
        }

        return HabitHeatmapResult(
            startDate = startDate,
            endDate = endDate,
            months = clampedMonths,
            habitId = habitId,
            totalActiveDays = totalActiveDays,
            cells = cells
        )
    }

    private fun roundOneDecimal(value: Double): Double =
        (value * 10.0).roundToInt() / 10.0

    private fun roundTwoDecimals(value: Double): Double =
        (value * 100.0).roundToInt() / 100.0
}
