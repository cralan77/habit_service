package com.habit.domain.model

import java.time.DayOfWeek
import java.time.Instant

data class UserRoutineSettings(
    val userId: String,
    val weekStartDay: DayOfWeek = DayOfWeek.MONDAY,
    val timezone: String = "America/Sao_Paulo",
    val eveningReminderEnabled: Boolean = true,
    val eveningReminderTime: String = "20:00",
    val updatedAt: Instant = Instant.now()
)
