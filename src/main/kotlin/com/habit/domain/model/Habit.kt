package com.habit.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Habit(
    val id: UUID = UUID.randomUUID(),
    val userId: String,
    val title: String,
    val description: String? = null,
    val icon: String = "check-circle",
    val colorHex: String = "#10B981",
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val daysOfMonth: Set<Int> = emptySet(),
    val targetCountPerPeriod: Int = 1,
    val quotaCountMode: QuotaCountMode = QuotaCountMode.DAYS_COMPLETED,
    val completionMode: CompletionMode = CompletionMode.CHECK,
    val dailyTargetValue: Double = 1.0,
    val unit: String? = null,
    val linkedTodoCategoryId: UUID? = null,
    val healthTriggerType: HealthTriggerType? = null,
    val healthExerciseType: String? = null,
    val healthMinDurationMinutes: Int? = null,
    val isActive: Boolean = true,
    val startDate: LocalDate = LocalDate.now(),
    val archivedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    fun isFixedSchedule(): Boolean =
        scheduleType == ScheduleType.DAILY ||
            scheduleType == ScheduleType.FIXED_WEEKLY ||
            scheduleType == ScheduleType.FIXED_MONTHLY

    fun isFlexibleSchedule(): Boolean =
        scheduleType == ScheduleType.FLEXIBLE_WEEKLY ||
            scheduleType == ScheduleType.FLEXIBLE_MONTHLY
}
