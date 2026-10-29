package com.habit.application.settings

import com.habit.domain.model.UserRoutineSettings
import com.habit.domain.repository.UserRoutineSettingsRepository
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.Instant

open class ManageUserRoutineSettingsUseCase(
    private val settingsRepository: UserRoutineSettingsRepository
) {

    @Transactional(readOnly = true)
    open fun getSettings(userId: String): UserRoutineSettings {
        return settingsRepository.findByUserId(userId)
    }

    @Transactional
    open fun updateSettings(
        userId: String,
        weekStartDay: DayOfWeek?,
        timezone: String?,
        eveningReminderEnabled: Boolean?,
        eveningReminderTime: String?
    ): UserRoutineSettings {
        val current = settingsRepository.findByUserId(userId)
        val updated = current.copy(
            weekStartDay = weekStartDay ?: current.weekStartDay,
            timezone = timezone?.takeIf { it.isNotBlank() } ?: current.timezone,
            eveningReminderEnabled = eveningReminderEnabled ?: current.eveningReminderEnabled,
            eveningReminderTime = eveningReminderTime?.takeIf { it.isNotBlank() } ?: current.eveningReminderTime,
            updatedAt = Instant.now()
        )
        return settingsRepository.save(updated)
    }
}
