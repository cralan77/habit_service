package com.habit.interfaces.rest.dto

import com.habit.application.habit.HabitWithStats
import com.habit.domain.model.CompletionMode
import com.habit.domain.model.HabitLogSource
import com.habit.domain.model.HealthTriggerType
import com.habit.domain.model.QuotaCountMode
import com.habit.domain.model.ScheduleType
import com.habit.domain.model.StreakUnit
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreateOrUpdateHabitRequest(
    val title: String,
    val description: String? = null,
    val icon: String? = "check-circle",
    val colorHex: String? = "#10B981",
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    val daysOfWeek: Set<DayOfWeek>? = null,
    val daysOfMonth: Set<Int>? = null,
    val targetCountPerPeriod: Int? = 1,
    val quotaCountMode: QuotaCountMode? = null,
    val completionMode: CompletionMode? = CompletionMode.CHECK,
    val dailyTargetValue: Double? = 1.0,
    val unit: String? = null,
    val linkedTodoCategoryId: UUID? = null,
    val healthTriggerType: HealthTriggerType? = null,
    val healthExerciseType: String? = null,
    val healthMinDurationMinutes: Int? = null,
    val startDate: LocalDate? = null
)

data class CheckHabitRequest(
    val date: LocalDate? = null,
    val value: Double? = null,
    val notes: String? = null,
    val source: HabitLogSource? = HabitLogSource.MANUAL
)

data class SkipHabitRequest(
    val date: LocalDate? = null,
    val skipCount: Int? = 1,
    val reason: String? = null
)

data class ArchiveHabitRequest(
    val archived: Boolean? = null
)

data class InternalHealthEventRequest(
    val userId: String,
    val eventType: HealthTriggerType,
    val date: LocalDate? = null,
    val value: Double? = 1.0,
    val durationMinutes: Int? = null,
    val exerciseType: String? = null,
    val referenceId: String? = null
)

data class HabitResponse(
    val id: UUID,
    val userId: String,
    val title: String,
    val description: String?,
    val icon: String,
    val colorHex: String,
    val scheduleType: ScheduleType,
    val daysOfWeek: Set<DayOfWeek>,
    val daysOfMonth: Set<Int>,
    val targetCountPerPeriod: Int,
    val quotaCountMode: QuotaCountMode,
    val completionMode: CompletionMode,
    val dailyTargetValue: Double,
    val unit: String?,
    val linkedTodoCategoryId: UUID?,
    val linkedTodoCategoryName: String?,
    val healthTriggerType: HealthTriggerType?,
    val healthExerciseType: String?,
    val healthMinDurationMinutes: Int?,
    val isActive: Boolean,
    val startDate: LocalDate,
    val archivedAt: Instant?,
    val currentStreak: Int,
    val bestStreak: Int,
    val streakUnit: StreakUnit,
    val periodTargetCount: Int,
    val periodAchievedCount: Int,
    val completedToday: Boolean,
    val currentValueToday: Double,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(stats: HabitWithStats) = HabitResponse(
            id = stats.habit.id,
            userId = stats.habit.userId,
            title = stats.habit.title,
            description = stats.habit.description,
            icon = stats.habit.icon,
            colorHex = stats.habit.colorHex,
            scheduleType = stats.habit.scheduleType,
            daysOfWeek = stats.habit.daysOfWeek,
            daysOfMonth = stats.habit.daysOfMonth,
            targetCountPerPeriod = stats.habit.targetCountPerPeriod,
            quotaCountMode = stats.habit.quotaCountMode,
            completionMode = stats.habit.completionMode,
            dailyTargetValue = stats.habit.dailyTargetValue,
            unit = stats.habit.unit,
            linkedTodoCategoryId = stats.habit.linkedTodoCategoryId,
            linkedTodoCategoryName = stats.linkedTodoCategoryName,
            healthTriggerType = stats.habit.healthTriggerType,
            healthExerciseType = stats.habit.healthExerciseType,
            healthMinDurationMinutes = stats.habit.healthMinDurationMinutes,
            isActive = stats.habit.isActive,
            startDate = stats.habit.startDate,
            archivedAt = stats.habit.archivedAt,
            currentStreak = stats.currentStreak,
            bestStreak = stats.bestStreak,
            streakUnit = stats.streakUnit,
            periodTargetCount = stats.periodTargetCount,
            periodAchievedCount = stats.periodAchievedCount,
            completedToday = stats.completedToday,
            currentValueToday = stats.currentValueToday,
            createdAt = stats.habit.createdAt,
            updatedAt = stats.habit.updatedAt
        )
    }
}
