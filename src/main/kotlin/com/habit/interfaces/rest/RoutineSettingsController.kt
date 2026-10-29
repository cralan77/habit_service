package com.habit.interfaces.rest

import com.habit.application.settings.ManageUserRoutineSettingsUseCase
import com.habit.interfaces.rest.dto.RoutineSettingsResponse
import com.habit.interfaces.rest.dto.UpdateRoutineSettingsRequest
import com.habit.interfaces.rest.security.HabitUserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/routines/settings")
class RoutineSettingsController(
    private val manageUserRoutineSettingsUseCase: ManageUserRoutineSettingsUseCase
) {

    @GetMapping
    fun getSettings(
        @AuthenticationPrincipal principal: HabitUserPrincipal?
    ): ResponseEntity<RoutineSettingsResponse> {
        val userId = principal?.userId ?: "default_mobile_user"
        val settings = manageUserRoutineSettingsUseCase.getSettings(userId)
        return ResponseEntity.ok(RoutineSettingsResponse.from(settings))
    }

    @PutMapping
    fun updateSettings(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @RequestBody request: UpdateRoutineSettingsRequest
    ): ResponseEntity<RoutineSettingsResponse> {
        val userId = principal?.userId ?: "default_mobile_user"
        val updated = manageUserRoutineSettingsUseCase.updateSettings(
            userId = userId,
            weekStartDay = request.weekStartDay,
            timezone = request.timezone,
            eveningReminderEnabled = request.eveningReminderEnabled,
            eveningReminderTime = request.eveningReminderTime
        )
        return ResponseEntity.ok(RoutineSettingsResponse.from(updated))
    }
}
