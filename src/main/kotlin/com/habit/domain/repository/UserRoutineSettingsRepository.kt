package com.habit.domain.repository

import com.habit.domain.model.UserRoutineSettings

interface UserRoutineSettingsRepository {
    fun findByUserId(userId: String): UserRoutineSettings
    fun save(settings: UserRoutineSettings): UserRoutineSettings
}
