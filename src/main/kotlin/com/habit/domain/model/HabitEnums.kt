package com.habit.domain.model

enum class ScheduleType {
    DAILY,
    FIXED_WEEKLY,
    FLEXIBLE_WEEKLY,
    FIXED_MONTHLY,
    FLEXIBLE_MONTHLY
}

enum class QuotaCountMode {
    DAYS_COMPLETED,
    TOTAL_EXECUTIONS
}

enum class CompletionMode {
    CHECK,
    QUANTITATIVE,
    TODO_LINKED,
    HEALTH_AUTO
}

enum class HealthTriggerType {
    WORKOUT_SESSION,
    DAILY_STEPS,
    WEIGHT_OR_BODY_LOGGED
}

enum class HabitLogStatus {
    COMPLETED,
    PARTIAL
}

enum class HabitLogSource {
    MANUAL,
    TODO_LINKED,
    HEALTH_AUTO,
    AI_ASSISTANT
}

enum class StreakUnit {
    DAYS,
    WEEKS,
    MONTHS
}

enum class PeriodType {
    WEEK,
    MONTH
}

enum class HabitStatusToday {
    PENDING,
    PARTIAL,
    COMPLETED,
    SKIPPED
}

enum class HabitPeriodStatusBadge {
    ON_TRACK,
    COMPLETED,
    OVERACHIEVED,
    BEHIND,
    SKIPPED
}
