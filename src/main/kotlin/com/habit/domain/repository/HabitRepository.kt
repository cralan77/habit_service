package com.habit.domain.repository

import com.habit.domain.model.Habit
import java.util.UUID

interface HabitRepository {
    fun save(habit: Habit): Habit
    fun findById(id: UUID): Habit?
    fun findByUserId(userId: String, includeArchived: Boolean = false): List<Habit>
    fun findByLinkedTodoCategoryId(userId: String, categoryId: UUID): List<Habit>
    fun deleteById(id: UUID)
}
