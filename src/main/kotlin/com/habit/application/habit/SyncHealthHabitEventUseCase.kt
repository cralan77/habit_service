package com.habit.application.habit

import com.habit.domain.model.CompletionMode
import com.habit.domain.model.Habit
import com.habit.domain.model.HabitExecutionLog
import com.habit.domain.model.HabitLogSource
import com.habit.domain.model.HabitLogStatus
import com.habit.domain.model.HealthTriggerType
import com.habit.domain.repository.HabitExecutionLogRepository
import com.habit.domain.repository.HabitRepository
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class HealthEventPayload(
    val userId: String,
    val eventType: HealthTriggerType,
    val date: LocalDate = LocalDate.now(),
    val value: Double = 1.0,
    val durationMinutes: Int? = null,
    val exerciseType: String? = null,
    val referenceId: String? = null
)

open class SyncHealthHabitEventUseCase(
    private val habitRepository: HabitRepository,
    private val logRepository: HabitExecutionLogRepository,
    private val healthServiceUrl: String = "http://localhost:8081"
) {
    private val logger = LoggerFactory.getLogger(SyncHealthHabitEventUseCase::class.java)
    private val restClient: RestClient by lazy {
        RestClient.builder().baseUrl(healthServiceUrl).build()
    }

    @Transactional
    open fun processHealthEvent(event: HealthEventPayload): List<Habit> {
        val healthHabits = habitRepository.findByUserId(event.userId, includeArchived = false)
            .filter { it.isActive && it.completionMode == CompletionMode.HEALTH_AUTO && it.healthTriggerType == event.eventType }

        if (healthHabits.isEmpty()) return emptyList()

        val updatedHabits = mutableListOf<Habit>()

        for (habit in healthHabits) {
            when (event.eventType) {
                HealthTriggerType.WORKOUT_SESSION -> {
                    val minDur = habit.healthMinDurationMinutes ?: 0
                    val actualDur = event.durationMinutes ?: Int.MAX_VALUE
                    val requiredType = habit.healthExerciseType?.uppercase()
                    val actualType = event.exerciseType?.uppercase()

                    val typeMatches = requiredType.isNullOrBlank() ||
                        requiredType == "ANY" ||
                        actualType == null ||
                        actualType.contains(requiredType)

                    if (actualDur >= minDur && typeMatches) {
                        val refId = event.referenceId ?: "workout-${event.date}"
                        val existing = logRepository.findByHabitIdAndSourceAndReferenceId(
                            habit.id,
                            HabitLogSource.HEALTH_AUTO,
                            refId
                        )
                        if (existing == null) {
                            logRepository.save(
                                HabitExecutionLog(
                                    id = UUID.randomUUID(),
                                    habitId = habit.id,
                                    userId = habit.userId,
                                    executionDate = event.date,
                                    completedValue = 1.0,
                                    status = HabitLogStatus.COMPLETED,
                                    source = HabitLogSource.HEALTH_AUTO,
                                    sourceReferenceId = refId,
                                    notes = "Auto-check via treino (${event.exerciseType ?: "Workout"})",
                                    createdAt = Instant.now(),
                                    updatedAt = Instant.now()
                                )
                            )
                        }
                        updatedHabits.add(habit)
                    }
                }

                HealthTriggerType.DAILY_STEPS -> {
                    val refId = "steps-${event.date}"
                    val target = if (habit.dailyTargetValue > 0.0) habit.dailyTargetValue else 8000.0
                    val status = if (event.value >= target) HabitLogStatus.COMPLETED else HabitLogStatus.PARTIAL
                    val existing = logRepository.findByHabitIdAndSourceAndReferenceId(
                        habit.id,
                        HabitLogSource.HEALTH_AUTO,
                        refId
                    )
                    if (existing != null) {
                        logRepository.save(
                            existing.copy(
                                completedValue = event.value,
                                status = status,
                                updatedAt = Instant.now()
                            )
                        )
                    } else {
                        logRepository.save(
                            HabitExecutionLog(
                                id = UUID.randomUUID(),
                                habitId = habit.id,
                                userId = habit.userId,
                                executionDate = event.date,
                                completedValue = event.value,
                                status = status,
                                source = HabitLogSource.HEALTH_AUTO,
                                sourceReferenceId = refId,
                                notes = "Auto-check de passos diários (${event.value.toInt()} passos)",
                                createdAt = Instant.now(),
                                updatedAt = Instant.now()
                            )
                        )
                    }
                    updatedHabits.add(habit)
                }

                HealthTriggerType.WEIGHT_OR_BODY_LOGGED -> {
                    val refId = event.referenceId ?: "body-${event.date}"
                    val existing = logRepository.findByHabitIdAndSourceAndReferenceId(
                        habit.id,
                        HabitLogSource.HEALTH_AUTO,
                        refId
                    )
                    if (existing == null) {
                        logRepository.save(
                            HabitExecutionLog(
                                id = UUID.randomUUID(),
                                habitId = habit.id,
                                userId = habit.userId,
                                executionDate = event.date,
                                completedValue = 1.0,
                                status = HabitLogStatus.COMPLETED,
                                source = HabitLogSource.HEALTH_AUTO,
                                sourceReferenceId = refId,
                                notes = "Auto-check via registro de peso/medidas corporais",
                                createdAt = Instant.now(),
                                updatedAt = Instant.now()
                            )
                        )
                    }
                    updatedHabits.add(habit)
                }
            }
        }

        return updatedHabits
    }

    /**
     * Pull / Reconciliação sob demanda consultando GET /api/health/summary no health_service.
     */
    @Transactional
    open fun reconcileFromHealthService(userId: String, date: LocalDate = LocalDate.now()): List<Habit> {
        val updated = mutableListOf<Habit>()
        try {
            @Suppress("UNCHECKED_CAST")
            val response = restClient.get()
                .uri("/api/health/summary?date=$date")
                .header("X-User-Id", userId)
                .retrieve()
                .body(Map::class.java) as? Map<String, Any?> ?: return emptyList()

            val stepsToday = (response["todaySteps"] as? Number)?.toDouble()
                ?: (response["stepsToday"] as? Number)?.toDouble()
                ?: (response["totalSteps"] as? Number)?.toDouble()
            if (stepsToday != null && stepsToday > 0) {
                updated.addAll(
                    processHealthEvent(
                        HealthEventPayload(
                            userId = userId,
                            eventType = HealthTriggerType.DAILY_STEPS,
                            date = date,
                            value = stepsToday,
                            referenceId = "steps-$date"
                        )
                    )
                )
            }

            val workoutsCount = (response["workoutsCountThisWeek"] as? Number)?.toInt()
                ?: (response["workoutsToday"] as? Number)?.toInt() ?: 0
            val latestWorkout = response["latestWorkout"] as? Map<*, *>
            if (latestWorkout != null) {
                val workoutDateStr = (latestWorkout["date"] ?: latestWorkout["startTime"])?.toString()?.take(10)
                val workoutDate = runCatching { LocalDate.parse(workoutDateStr) }.getOrDefault(date)
                val durationMin = (latestWorkout["durationMinutes"] as? Number)?.toInt() ?: 30
                val exerciseType = latestWorkout["exerciseType"]?.toString() ?: "ANY"
                val workoutId = latestWorkout["id"]?.toString() ?: "workout-$workoutDate"
                updated.addAll(
                    processHealthEvent(
                        HealthEventPayload(
                            userId = userId,
                            eventType = HealthTriggerType.WORKOUT_SESSION,
                            date = workoutDate,
                            value = 1.0,
                            durationMinutes = durationMin,
                            exerciseType = exerciseType,
                            referenceId = workoutId
                        )
                    )
                )
            } else if (workoutsCount > 0) {
                updated.addAll(
                    processHealthEvent(
                        HealthEventPayload(
                            userId = userId,
                            eventType = HealthTriggerType.WORKOUT_SESSION,
                            date = date,
                            value = 1.0,
                            durationMinutes = 30,
                            exerciseType = "ANY",
                            referenceId = "workout-summary-$date"
                        )
                    )
                )
            }

            val latestWeight = response["latestWeightKg"] ?: response["currentWeightKg"]
            val latestWeightDateStr = response["latestWeightDate"]?.toString()?.take(10)
            if (latestWeight != null && (latestWeightDateStr == null || latestWeightDateStr == date.toString())) {
                updated.addAll(
                    processHealthEvent(
                        HealthEventPayload(
                            userId = userId,
                            eventType = HealthTriggerType.WEIGHT_OR_BODY_LOGGED,
                            date = date,
                            value = 1.0,
                            referenceId = "weight-$date"
                        )
                    )
                )
            }
        } catch (ex: Exception) {
            logger.warn("Não foi possível reconciliar dados com health_service em {}: {}", healthServiceUrl, ex.message)
        }
        return updated.distinctBy { it.id }
    }
}
