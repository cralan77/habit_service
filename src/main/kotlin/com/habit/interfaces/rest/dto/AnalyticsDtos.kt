package com.habit.interfaces.rest.dto

import com.habit.domain.model.UserRoutineSettings
import java.time.DayOfWeek
import java.time.Instant

data class UpdateRoutineSettingsRequest(
    val weekStartDay: DayOfWeek? = null,
    val timezone: String? = null,
    val eveningReminderEnabled: Boolean? = null,
    val eveningReminderTime: String? = null
)

data class RoutineSettingsResponse(
    val userId: String,
    val weekStartDay: DayOfWeek,
    val timezone: String,
    val eveningReminderEnabled: Boolean,
    val eveningReminderTime: String,
    val updatedAt: Instant
) {
    companion object {
        fun from(domain: UserRoutineSettings) = RoutineSettingsResponse(
            userId = domain.userId,
            weekStartDay = domain.weekStartDay,
            timezone = domain.timezone,
            eveningReminderEnabled = domain.eveningReminderEnabled,
            eveningReminderTime = domain.eveningReminderTime,
            updatedAt = domain.updatedAt
        )
    }
}
