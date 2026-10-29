package com.habit.infrastructure.persistence

import com.habit.domain.model.CompletionMode
import com.habit.domain.model.Habit
import com.habit.domain.model.HabitExecutionLog
import com.habit.domain.model.HabitLogSource
import com.habit.domain.model.HabitLogStatus
import com.habit.domain.model.HabitPeriodSkip
import com.habit.domain.model.HealthTriggerType
import com.habit.domain.model.QuotaCountMode
import com.habit.domain.model.ScheduleType
import com.habit.domain.model.TodoCategory
import com.habit.domain.model.TodoItem
import com.habit.domain.model.TodoPriority
import com.habit.domain.model.TodoStatus
import com.habit.domain.model.TodoSubtask
import com.habit.domain.model.UserRoutineSettings
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "user_routine_settings")
class UserRoutineSettingsEntity(
    @Id
    @Column(name = "user_id", nullable = false, length = 128)
    var userId: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "week_start_day", nullable = false, length = 16)
    var weekStartDay: DayOfWeek = DayOfWeek.MONDAY,

    @Column(name = "timezone", nullable = false, length = 64)
    var timezone: String = "America/Sao_Paulo",

    @Column(name = "evening_reminder_enabled", nullable = false)
    var eveningReminderEnabled: Boolean = true,

    @Column(name = "evening_reminder_time", nullable = false, length = 8)
    var eveningReminderTime: String = "20:00",

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    fun toDomain() = UserRoutineSettings(
        userId = userId,
        weekStartDay = weekStartDay,
        timezone = timezone,
        eveningReminderEnabled = eveningReminderEnabled,
        eveningReminderTime = eveningReminderTime,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(domain: UserRoutineSettings) = UserRoutineSettingsEntity(
            userId = domain.userId,
            weekStartDay = domain.weekStartDay,
            timezone = domain.timezone,
            eveningReminderEnabled = domain.eveningReminderEnabled,
            eveningReminderTime = domain.eveningReminderTime,
            updatedAt = domain.updatedAt
        )
    }
}

@Entity
@Table(name = "todo_categories")
class TodoCategoryEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false, length = 128)
    var userId: String = "",

    @Column(name = "name", nullable = false, length = 100)
    var name: String = "",

    @Column(name = "color_hex", nullable = false, length = 16)
    var colorHex: String = "#6366F1",

    @Column(name = "icon", nullable = false, length = 50)
    var icon: String = "folder",

    @Column(name = "is_archived", nullable = false)
    var isArchived: Boolean = false,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
) {
    fun toDomain() = TodoCategory(
        id = id,
        userId = userId,
        name = name,
        colorHex = colorHex,
        icon = icon,
        isArchived = isArchived,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(domain: TodoCategory) = TodoCategoryEntity(
            id = domain.id,
            userId = domain.userId,
            name = domain.name,
            colorHex = domain.colorHex,
            icon = domain.icon,
            isArchived = domain.isArchived,
            createdAt = domain.createdAt
        )
    }
}

@Entity
@Table(name = "habits")
class HabitEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false, length = 128)
    var userId: String = "",

    @Column(name = "title", nullable = false, length = 160)
    var title: String = "",

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "icon", nullable = false, length = 50)
    var icon: String = "check-circle",

    @Column(name = "color_hex", nullable = false, length = 16)
    var colorHex: String = "#10B981",

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 32)
    var scheduleType: ScheduleType = ScheduleType.DAILY,

    @Column(name = "days_of_week", length = 64)
    var daysOfWeekCsv: String? = null,

    @Column(name = "days_of_month", length = 128)
    var daysOfMonthCsv: String? = null,

    @Column(name = "target_count_per_period", nullable = false)
    var targetCountPerPeriod: Int = 1,

    @Enumerated(EnumType.STRING)
    @Column(name = "quota_count_mode", nullable = false, length = 32)
    var quotaCountMode: QuotaCountMode = QuotaCountMode.DAYS_COMPLETED,

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_mode", nullable = false, length = 32)
    var completionMode: CompletionMode = CompletionMode.CHECK,

    @Column(name = "daily_target_value", nullable = false)
    var dailyTargetValue: Double = 1.0,

    @Column(name = "unit", length = 32)
    var unit: String? = null,

    @Column(name = "linked_todo_category_id")
    var linkedTodoCategoryId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "health_trigger_type", length = 32)
    var healthTriggerType: HealthTriggerType? = null,

    @Column(name = "health_exercise_type", length = 64)
    var healthExerciseType: String? = null,

    @Column(name = "health_min_duration_minutes")
    var healthMinDurationMinutes: Int? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate = LocalDate.now(),

    @Column(name = "archived_at")
    var archivedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    fun toDomain(): Habit {
        val parsedDaysOfWeek = daysOfWeekCsv
            ?.split(",")
            ?.mapNotNull { token ->
                val clean = token.trim()
                if (clean.isEmpty()) null else runCatching { DayOfWeek.valueOf(clean) }.getOrNull()
            }
            ?.toSet() ?: emptySet()

        val parsedDaysOfMonth = daysOfMonthCsv
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.toSet() ?: emptySet()

        return Habit(
            id = id,
            userId = userId,
            title = title,
            description = description,
            icon = icon,
            colorHex = colorHex,
            scheduleType = scheduleType,
            daysOfWeek = parsedDaysOfWeek,
            daysOfMonth = parsedDaysOfMonth,
            targetCountPerPeriod = targetCountPerPeriod,
            quotaCountMode = quotaCountMode,
            completionMode = completionMode,
            dailyTargetValue = dailyTargetValue,
            unit = unit,
            linkedTodoCategoryId = linkedTodoCategoryId,
            healthTriggerType = healthTriggerType,
            healthExerciseType = healthExerciseType,
            healthMinDurationMinutes = healthMinDurationMinutes,
            isActive = isActive,
            startDate = startDate,
            archivedAt = archivedAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(domain: Habit) = HabitEntity(
            id = domain.id,
            userId = domain.userId,
            title = domain.title,
            description = domain.description,
            icon = domain.icon,
            colorHex = domain.colorHex,
            scheduleType = domain.scheduleType,
            daysOfWeekCsv = domain.daysOfWeek.takeIf { it.isNotEmpty() }?.joinToString(",") { it.name },
            daysOfMonthCsv = domain.daysOfMonth.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(","),
            targetCountPerPeriod = domain.targetCountPerPeriod,
            quotaCountMode = domain.quotaCountMode,
            completionMode = domain.completionMode,
            dailyTargetValue = domain.dailyTargetValue,
            unit = domain.unit,
            linkedTodoCategoryId = domain.linkedTodoCategoryId,
            healthTriggerType = domain.healthTriggerType,
            healthExerciseType = domain.healthExerciseType,
            healthMinDurationMinutes = domain.healthMinDurationMinutes,
            isActive = domain.isActive,
            startDate = domain.startDate,
            archivedAt = domain.archivedAt,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}

@Entity
@Table(name = "habit_execution_logs")
class HabitExecutionLogEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "habit_id", nullable = false)
    var habitId: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false, length = 128)
    var userId: String = "",

    @Column(name = "execution_date", nullable = false)
    var executionDate: LocalDate = LocalDate.now(),

    @Column(name = "completed_value", nullable = false)
    var completedValue: Double = 1.0,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    var status: HabitLogStatus = HabitLogStatus.COMPLETED,

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 24)
    var source: HabitLogSource = HabitLogSource.MANUAL,

    @Column(name = "source_reference_id", length = 128)
    var sourceReferenceId: String? = null,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    fun toDomain() = HabitExecutionLog(
        id = id,
        habitId = habitId,
        userId = userId,
        executionDate = executionDate,
        completedValue = completedValue,
        status = status,
        source = source,
        sourceReferenceId = sourceReferenceId,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(domain: HabitExecutionLog) = HabitExecutionLogEntity(
            id = domain.id,
            habitId = domain.habitId,
            userId = domain.userId,
            executionDate = domain.executionDate,
            completedValue = domain.completedValue,
            status = domain.status,
            source = domain.source,
            sourceReferenceId = domain.sourceReferenceId,
            notes = domain.notes,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}

@Entity
@Table(name = "habit_period_skips")
class HabitPeriodSkipEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "habit_id", nullable = false)
    var habitId: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false, length = 128)
    var userId: String = "",

    @Column(name = "skip_date", nullable = false)
    var skipDate: LocalDate = LocalDate.now(),

    @Column(name = "skip_count", nullable = false)
    var skipCount: Int = 1,

    @Column(name = "reason", length = 255)
    var reason: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
) {
    fun toDomain() = HabitPeriodSkip(
        id = id,
        habitId = habitId,
        userId = userId,
        skipDate = skipDate,
        skipCount = skipCount,
        reason = reason,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(domain: HabitPeriodSkip) = HabitPeriodSkipEntity(
            id = domain.id,
            habitId = domain.habitId,
            userId = domain.userId,
            skipDate = domain.skipDate,
            skipCount = domain.skipCount,
            reason = domain.reason,
            createdAt = domain.createdAt
        )
    }
}

@Entity
@Table(name = "todo_items")
class TodoItemEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false, length = 128)
    var userId: String = "",

    @Column(name = "title", nullable = false, length = 200)
    var title: String = "",

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "category_id")
    var categoryId: UUID? = null,

    @Column(name = "linked_habit_id")
    var linkedHabitId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 16)
    var priority: TodoPriority = TodoPriority.MEDIUM,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: TodoStatus = TodoStatus.PENDING,

    @Column(name = "due_date")
    var dueDate: LocalDate? = null,

    @Column(name = "completed_at")
    var completedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    fun toDomain(subtasks: List<TodoSubtask>) = TodoItem(
        id = id,
        userId = userId,
        title = title,
        description = description,
        categoryId = categoryId,
        linkedHabitId = linkedHabitId,
        priority = priority,
        status = status,
        dueDate = dueDate,
        completedAt = completedAt,
        subtasks = subtasks,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(domain: TodoItem) = TodoItemEntity(
            id = domain.id,
            userId = domain.userId,
            title = domain.title,
            description = domain.description,
            categoryId = domain.categoryId,
            linkedHabitId = domain.linkedHabitId,
            priority = domain.priority,
            status = domain.status,
            dueDate = domain.dueDate,
            completedAt = domain.completedAt,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}

@Entity
@Table(name = "todo_subtasks")
class TodoSubtaskEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "todo_id", nullable = false)
    var todoId: UUID = UUID.randomUUID(),

    @Column(name = "title", nullable = false, length = 200)
    var title: String = "",

    @Column(name = "is_completed", nullable = false)
    var isCompleted: Boolean = false,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(name = "completed_at")
    var completedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
) {
    fun toDomain() = TodoSubtask(
        id = id,
        todoId = todoId,
        title = title,
        isCompleted = isCompleted,
        sortOrder = sortOrder,
        completedAt = completedAt,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(domain: TodoSubtask) = TodoSubtaskEntity(
            id = domain.id,
            todoId = domain.todoId,
            title = domain.title,
            isCompleted = domain.isCompleted,
            sortOrder = domain.sortOrder,
            completedAt = domain.completedAt,
            createdAt = domain.createdAt
        )
    }
}
