package com.habit.interfaces.rest

import com.habit.application.analytics.GetDailyRoutineViewUseCase
import com.habit.application.analytics.GetExpectedVsAchievedUseCase
import com.habit.application.analytics.GetHabitHeatmapUseCase
import com.habit.application.analytics.GetRoutineSummaryForAiUseCase
import com.habit.domain.model.DailyRoutineView
import com.habit.domain.model.ExpectedVsAchievedComparison
import com.habit.domain.model.HabitHeatmapResult
import com.habit.domain.model.PeriodType
import com.habit.domain.model.RoutineAiSummary
import com.habit.interfaces.rest.security.HabitUserPrincipal
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/routines")
class RoutineAnalyticsController(
    private val getDailyRoutineViewUseCase: GetDailyRoutineViewUseCase,
    private val getExpectedVsAchievedUseCase: GetExpectedVsAchievedUseCase,
    private val getHabitHeatmapUseCase: GetHabitHeatmapUseCase,
    private val getRoutineSummaryForAiUseCase: GetRoutineSummaryForAiUseCase
) {

    @GetMapping("/daily")
    fun getDailyRoutine(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?
    ): ResponseEntity<DailyRoutineView> {
        val userId = principal?.userId ?: "default_mobile_user"
        val view = getDailyRoutineViewUseCase.execute(userId, date ?: LocalDate.now())
        return ResponseEntity.ok(view)
    }

    @GetMapping("/analytics/comparison")
    fun getComparison(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @RequestParam(defaultValue = "WEEK") period: PeriodType,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) referenceDate: LocalDate?
    ): ResponseEntity<ExpectedVsAchievedComparison> {
        val userId = principal?.userId ?: "default_mobile_user"
        val comparison = getExpectedVsAchievedUseCase.execute(
            userId = userId,
            period = period,
            referenceDate = referenceDate ?: LocalDate.now()
        )
        return ResponseEntity.ok(comparison)
    }

    @GetMapping("/analytics/heatmap")
    fun getHeatmap(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @RequestParam(defaultValue = "3") months: Int,
        @RequestParam(required = false) habitId: UUID?
    ): ResponseEntity<HabitHeatmapResult> {
        val userId = principal?.userId ?: "default_mobile_user"
        val heatmap = getHabitHeatmapUseCase.execute(
            userId = userId,
            months = months,
            habitId = habitId
        )
        return ResponseEntity.ok(heatmap)
    }

    @GetMapping("/summary")
    fun getRoutineSummary(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?
    ): ResponseEntity<RoutineAiSummary> {
        val userId = principal?.userId ?: "default_mobile_user"
        val summary = getRoutineSummaryForAiUseCase.execute(userId, date ?: LocalDate.now())
        return ResponseEntity.ok(summary)
    }
}
