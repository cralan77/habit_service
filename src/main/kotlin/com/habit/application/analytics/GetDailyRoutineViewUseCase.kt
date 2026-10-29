package com.habit.application.analytics

import com.habit.domain.model.DailyRoutineView
import com.habit.domain.repository.HabitExecutionLogRepository
import com.habit.domain.repository.HabitPeriodSkipRepository
import com.habit.domain.repository.HabitRepository
import com.habit.domain.repository.TodoCategoryRepository
import com.habit.domain.repository.TodoItemRepository
import com.habit.domain.repository.UserRoutineSettingsRepository
import com.habit.domain.service.HabitAnalyticsEngineService
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

open class GetDailyRoutineViewUseCase(
    private val habitRepository: HabitRepository,
    private val logRepository: HabitExecutionLogRepository,
    private val skipRepository: HabitPeriodSkipRepository,
    private val todoRepository: TodoItemRepository,
    private val categoryRepository: TodoCategoryRepository,
    private val settingsRepository: UserRoutineSettingsRepository,
    private val analyticsEngine: HabitAnalyticsEngineService
) {

    @Transactional(readOnly = true)
    open fun execute(userId: String, date: LocalDate = LocalDate.now()): DailyRoutineView {
        val settings = settingsRepository.findByUserId(userId)
        val habits = habitRepository.findByUserId(userId, includeArchived = false)
        val earliestStart = if (habits.isEmpty()) date.minusMonths(3) else habits.minOf { it.startDate }.coerceAtMost(date.minusMonths(3))

        val logs = logRepository.findByUserIdAndDateRange(userId, earliestStart, date.plusDays(7))
        val skips = skipRepository.findByUserIdAndDateRange(userId, earliestStart, date.plusDays(7))
        val todos = todoRepository.findByUserId(userId)
        val categoriesById = categoryRepository.findByUserId(userId).associateBy { it.id }

        return analyticsEngine.buildDailyRoutineView(
            date = date,
            weekStartDay = settings.weekStartDay,
            habits = habits,
            logs = logs,
            skips = skips,
            todos = todos,
            categoriesById = categoriesById
        )
    }
}
