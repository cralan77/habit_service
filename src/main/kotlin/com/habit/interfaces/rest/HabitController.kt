package com.habit.interfaces.rest

import com.habit.application.habit.HealthEventPayload
import com.habit.application.habit.LogHabitExecutionUseCase
import com.habit.application.habit.ManageHabitUseCase
import com.habit.application.habit.SyncHealthHabitEventUseCase
import com.habit.domain.model.HabitLogSource
import com.habit.interfaces.rest.dto.ArchiveHabitRequest
import com.habit.interfaces.rest.dto.CheckHabitRequest
import com.habit.interfaces.rest.dto.CreateOrUpdateHabitRequest
import com.habit.interfaces.rest.dto.HabitResponse
import com.habit.interfaces.rest.dto.InternalHealthEventRequest
import com.habit.interfaces.rest.dto.SkipHabitRequest
import com.habit.interfaces.rest.security.HabitUserPrincipal
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/habits")
class HabitController(
    private val manageHabitUseCase: ManageHabitUseCase,
    private val logHabitExecutionUseCase: LogHabitExecutionUseCase,
    private val syncHealthHabitEventUseCase: SyncHealthHabitEventUseCase
) {

    @GetMapping
    fun listHabits(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @RequestParam(defaultValue = "false") includeArchived: Boolean
    ): ResponseEntity<List<HabitResponse>> {
        val userId = principal?.userId ?: "default_mobile_user"
        val habits = manageHabitUseCase.listHabits(userId, includeArchived)
            .map { HabitResponse.from(it) }
        return ResponseEntity.ok(habits)
    }

    @PostMapping
    fun createHabit(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @RequestBody request: CreateOrUpdateHabitRequest
    ): ResponseEntity<HabitResponse> {
        val userId = principal?.userId ?: "default_mobile_user"
        val created = manageHabitUseCase.createHabit(
            userId = userId,
            title = request.title,
            description = request.description,
            icon = request.icon,
            colorHex = request.colorHex,
            scheduleType = request.scheduleType,
            daysOfWeek = request.daysOfWeek,
            daysOfMonth = request.daysOfMonth,
            targetCountPerPeriod = request.targetCountPerPeriod,
            quotaCountMode = request.quotaCountMode,
            completionMode = request.completionMode,
            dailyTargetValue = request.dailyTargetValue,
            unit = request.unit,
            linkedTodoCategoryId = request.linkedTodoCategoryId,
            healthTriggerType = request.healthTriggerType,
            healthExerciseType = request.healthExerciseType,
            healthMinDurationMinutes = request.healthMinDurationMinutes,
            startDate = request.startDate
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(HabitResponse.from(created))
    }

    @GetMapping("/{id}")
    fun getHabit(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @PathVariable id: UUID
    ): ResponseEntity<HabitResponse> {
        val userId = principal?.userId ?: "default_mobile_user"
        val habit = manageHabitUseCase.getHabitById(userId, id)
        return ResponseEntity.ok(HabitResponse.from(habit))
    }

    @PutMapping("/{id}")
    fun updateHabit(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @PathVariable id: UUID,
        @RequestBody request: CreateOrUpdateHabitRequest
    ): ResponseEntity<HabitResponse> {
        val userId = principal?.userId ?: "default_mobile_user"
        val updated = manageHabitUseCase.updateHabit(
            userId = userId,
            habitId = id,
            title = request.title,
            description = request.description,
            icon = request.icon,
            colorHex = request.colorHex,
            scheduleType = request.scheduleType,
            daysOfWeek = request.daysOfWeek,
            daysOfMonth = request.daysOfMonth,
            targetCountPerPeriod = request.targetCountPerPeriod,
            quotaCountMode = request.quotaCountMode,
            completionMode = request.completionMode,
            dailyTargetValue = request.dailyTargetValue,
            unit = request.unit,
            linkedTodoCategoryId = request.linkedTodoCategoryId,
            healthTriggerType = request.healthTriggerType,
            healthExerciseType = request.healthExerciseType,
            healthMinDurationMinutes = request.healthMinDurationMinutes
        )
        return ResponseEntity.ok(HabitResponse.from(updated))
    }

    @PatchMapping("/{id}/archive")
    fun archiveHabit(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @PathVariable id: UUID,
        @RequestBody(required = false) request: ArchiveHabitRequest?
    ): ResponseEntity<HabitResponse> {
        val userId = principal?.userId ?: "default_mobile_user"
        val updated = manageHabitUseCase.archiveOrRestoreHabit(userId, id, request?.archived)
        return ResponseEntity.ok(HabitResponse.from(updated))
    }

    @DeleteMapping("/{id}")
    fun deleteHabit(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        val userId = principal?.userId ?: "default_mobile_user"
        manageHabitUseCase.deleteHabit(userId, id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/check")
    fun checkHabit(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @PathVariable id: UUID,
        @RequestBody(required = false) request: CheckHabitRequest?
    ): ResponseEntity<HabitResponse> {
        val userId = principal?.userId ?: "default_mobile_user"
        val updated = logHabitExecutionUseCase.checkHabit(
            userId = userId,
            habitId = id,
            date = request?.date ?: LocalDate.now(),
            value = request?.value,
            notes = request?.notes,
            source = request?.source ?: HabitLogSource.MANUAL
        )
        return ResponseEntity.ok(HabitResponse.from(updated))
    }

    @DeleteMapping("/{id}/check")
    fun uncheckHabit(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @PathVariable id: UUID,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?
    ): ResponseEntity<HabitResponse> {
        val userId = principal?.userId ?: "default_mobile_user"
        val updated = logHabitExecutionUseCase.uncheckHabit(
            userId = userId,
            habitId = id,
            date = date ?: LocalDate.now()
        )
        return ResponseEntity.ok(HabitResponse.from(updated))
    }

    @PostMapping("/{id}/skip")
    fun skipHabit(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @PathVariable id: UUID,
        @RequestBody(required = false) request: SkipHabitRequest?
    ): ResponseEntity<HabitResponse> {
        val userId = principal?.userId ?: "default_mobile_user"
        val updated = logHabitExecutionUseCase.skipHabit(
            userId = userId,
            habitId = id,
            date = request?.date ?: LocalDate.now(),
            skipCount = request?.skipCount ?: 1,
            reason = request?.reason
        )
        return ResponseEntity.ok(HabitResponse.from(updated))
    }

    @DeleteMapping("/{id}/skip")
    fun unskipHabit(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @PathVariable id: UUID,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?
    ): ResponseEntity<HabitResponse> {
        val userId = principal?.userId ?: "default_mobile_user"
        val updated = logHabitExecutionUseCase.unskipHabit(
            userId = userId,
            habitId = id,
            date = date ?: LocalDate.now()
        )
        return ResponseEntity.ok(HabitResponse.from(updated))
    }

    @PostMapping("/internal/health-events")
    fun receiveInternalHealthEvent(
        @RequestBody request: InternalHealthEventRequest
    ): ResponseEntity<Map<String, Any>> {
        val updated = syncHealthHabitEventUseCase.processHealthEvent(
            HealthEventPayload(
                userId = request.userId,
                eventType = request.eventType,
                date = request.date ?: LocalDate.now(),
                value = request.value ?: 1.0,
                durationMinutes = request.durationMinutes,
                exerciseType = request.exerciseType,
                referenceId = request.referenceId
            )
        )
        return ResponseEntity.ok(
            mapOf(
                "status" to "PROCESSED",
                "updatedHabitsCount" to updated.size,
                "updatedHabitTitles" to updated.map { it.title }
            )
        )
    }

    @PostMapping("/sync-health")
    fun syncHealthHabits(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?
    ): ResponseEntity<Map<String, Any>> {
        val userId = principal?.userId ?: "default_mobile_user"
        val updated = syncHealthHabitEventUseCase.reconcileFromHealthService(userId, date ?: LocalDate.now())
        return ResponseEntity.ok(
            mapOf(
                "status" to "SYNCED",
                "updatedHabitsCount" to updated.size,
                "updatedHabitTitles" to updated.map { it.title }
            )
        )
    }
}
