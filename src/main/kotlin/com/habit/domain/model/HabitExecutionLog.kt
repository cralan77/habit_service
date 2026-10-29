package com.habit.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class HabitExecutionLog(
    val id: UUID = UUID.randomUUID(),
    val habitId: UUID,
    val userId: String,
    val executionDate: LocalDate,
    val completedValue: Double = 1.0,
    val status: HabitLogStatus = HabitLogStatus.COMPLETED,
    val source: HabitLogSource = HabitLogSource.MANUAL,
    val sourceReferenceId: String? = null,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
