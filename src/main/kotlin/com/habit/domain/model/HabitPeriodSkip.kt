package com.habit.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class HabitPeriodSkip(
    val id: UUID = UUID.randomUUID(),
    val habitId: UUID,
    val userId: String,
    val skipDate: LocalDate,
    val skipCount: Int = 1,
    val reason: String? = null,
    val createdAt: Instant = Instant.now()
)
