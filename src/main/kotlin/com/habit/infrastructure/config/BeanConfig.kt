package com.habit.infrastructure.config

import com.habit.application.analytics.GetDailyRoutineViewUseCase
import com.habit.application.analytics.GetExpectedVsAchievedUseCase
import com.habit.application.analytics.GetHabitHeatmapUseCase
import com.habit.application.analytics.GetRoutineSummaryForAiUseCase
import com.habit.application.habit.LogHabitExecutionUseCase
import com.habit.application.habit.ManageHabitUseCase
import com.habit.application.habit.SyncHealthHabitEventUseCase
import com.habit.application.settings.ManageUserRoutineSettingsUseCase
import com.habit.application.todo.CompleteTodoItemUseCase
import com.habit.application.todo.ManageTodoCategoryUseCase
import com.habit.application.todo.ManageTodoItemUseCase
import com.habit.domain.repository.HabitExecutionLogRepository
import com.habit.domain.repository.HabitPeriodSkipRepository
import com.habit.domain.repository.HabitRepository
import com.habit.domain.repository.TodoCategoryRepository
import com.habit.domain.repository.TodoItemRepository
import com.habit.domain.repository.UserRoutineSettingsRepository
import com.habit.domain.service.HabitAnalyticsEngineService
import com.habit.domain.service.HabitScheduleEvaluatorService
import com.habit.domain.service.HabitStreakCalculatorService
import com.habit.domain.service.TodoHabitSyncDomainService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BeanConfig {

    @Bean
    fun habitScheduleEvaluatorService() = HabitScheduleEvaluatorService()

    @Bean
    fun habitStreakCalculatorService(
        scheduleEvaluator: HabitScheduleEvaluatorService
    ) = HabitStreakCalculatorService(scheduleEvaluator)

    @Bean
    fun habitAnalyticsEngineService(
        scheduleEvaluator: HabitScheduleEvaluatorService,
        streakCalculator: HabitStreakCalculatorService
    ) = HabitAnalyticsEngineService(scheduleEvaluator, streakCalculator)

    @Bean
    fun todoHabitSyncDomainService(
        habitRepository: HabitRepository,
        logRepository: HabitExecutionLogRepository
    ) = TodoHabitSyncDomainService(habitRepository, logRepository)

    @Bean
    fun manageHabitUseCase(
        habitRepository: HabitRepository,
        logRepository: HabitExecutionLogRepository,
        skipRepository: HabitPeriodSkipRepository,
        categoryRepository: TodoCategoryRepository,
        settingsRepository: UserRoutineSettingsRepository,
        scheduleEvaluator: HabitScheduleEvaluatorService,
        streakCalculator: HabitStreakCalculatorService
    ) = ManageHabitUseCase(
        habitRepository,
        logRepository,
        skipRepository,
        categoryRepository,
        settingsRepository,
        scheduleEvaluator,
        streakCalculator
    )

    @Bean
    fun logHabitExecutionUseCase(
        habitRepository: HabitRepository,
        logRepository: HabitExecutionLogRepository,
        skipRepository: HabitPeriodSkipRepository,
        manageHabitUseCase: ManageHabitUseCase
    ) = LogHabitExecutionUseCase(habitRepository, logRepository, skipRepository, manageHabitUseCase)

    @Bean
    fun syncHealthHabitEventUseCase(
        habitRepository: HabitRepository,
        logRepository: HabitExecutionLogRepository,
        @Value("\${health.service.url:http://localhost:8081}") healthServiceUrl: String
    ) = SyncHealthHabitEventUseCase(habitRepository, logRepository, healthServiceUrl)

    @Bean
    fun manageTodoCategoryUseCase(
        categoryRepository: TodoCategoryRepository
    ) = ManageTodoCategoryUseCase(categoryRepository)

    @Bean
    fun manageTodoItemUseCase(
        todoRepository: TodoItemRepository,
        categoryRepository: TodoCategoryRepository,
        habitRepository: HabitRepository,
        todoHabitSyncService: TodoHabitSyncDomainService
    ) = ManageTodoItemUseCase(todoRepository, categoryRepository, habitRepository, todoHabitSyncService)

    @Bean
    fun completeTodoItemUseCase(
        todoRepository: TodoItemRepository,
        todoHabitSyncService: TodoHabitSyncDomainService,
        manageTodoItemUseCase: ManageTodoItemUseCase
    ) = CompleteTodoItemUseCase(todoRepository, todoHabitSyncService, manageTodoItemUseCase)

    @Bean
    fun getDailyRoutineViewUseCase(
        habitRepository: HabitRepository,
        logRepository: HabitExecutionLogRepository,
        skipRepository: HabitPeriodSkipRepository,
        todoRepository: TodoItemRepository,
        categoryRepository: TodoCategoryRepository,
        settingsRepository: UserRoutineSettingsRepository,
        analyticsEngine: HabitAnalyticsEngineService
    ) = GetDailyRoutineViewUseCase(
        habitRepository,
        logRepository,
        skipRepository,
        todoRepository,
        categoryRepository,
        settingsRepository,
        analyticsEngine
    )

    @Bean
    fun getExpectedVsAchievedUseCase(
        habitRepository: HabitRepository,
        logRepository: HabitExecutionLogRepository,
        skipRepository: HabitPeriodSkipRepository,
        settingsRepository: UserRoutineSettingsRepository,
        analyticsEngine: HabitAnalyticsEngineService
    ) = GetExpectedVsAchievedUseCase(
        habitRepository,
        logRepository,
        skipRepository,
        settingsRepository,
        analyticsEngine
    )

    @Bean
    fun getHabitHeatmapUseCase(
        habitRepository: HabitRepository,
        logRepository: HabitExecutionLogRepository,
        skipRepository: HabitPeriodSkipRepository,
        analyticsEngine: HabitAnalyticsEngineService
    ) = GetHabitHeatmapUseCase(
        habitRepository,
        logRepository,
        skipRepository,
        analyticsEngine
    )

    @Bean
    fun getRoutineSummaryForAiUseCase(
        getDailyRoutineViewUseCase: GetDailyRoutineViewUseCase,
        getExpectedVsAchievedUseCase: GetExpectedVsAchievedUseCase,
        todoRepository: TodoItemRepository
    ) = GetRoutineSummaryForAiUseCase(
        getDailyRoutineViewUseCase,
        getExpectedVsAchievedUseCase,
        todoRepository
    )

    @Bean
    fun manageUserRoutineSettingsUseCase(
        settingsRepository: UserRoutineSettingsRepository
    ) = ManageUserRoutineSettingsUseCase(settingsRepository)
}
