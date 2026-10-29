package com.habit.application

import com.habit.application.analytics.GetDailyRoutineViewUseCase
import com.habit.application.analytics.GetExpectedVsAchievedUseCase
import com.habit.application.analytics.GetHabitHeatmapUseCase
import com.habit.application.analytics.GetRoutineSummaryForAiUseCase
import com.habit.application.habit.HealthEventPayload
import com.habit.application.habit.LogHabitExecutionUseCase
import com.habit.application.habit.ManageHabitUseCase
import com.habit.application.habit.SyncHealthHabitEventUseCase
import com.habit.application.settings.ManageUserRoutineSettingsUseCase
import com.habit.application.todo.CompleteTodoItemUseCase
import com.habit.application.todo.ManageTodoCategoryUseCase
import com.habit.application.todo.ManageTodoItemUseCase
import com.habit.domain.model.CompletionMode
import com.habit.domain.model.HealthTriggerType
import com.habit.domain.model.PeriodType
import com.habit.domain.model.QuotaCountMode
import com.habit.domain.model.ScheduleType
import com.habit.domain.model.TodoPriority
import com.habit.domain.model.TodoStatus
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate

@SpringBootTest
@Transactional
class HabitUseCasesIntegrationTest {

    @Autowired
    private lateinit var manageHabitUseCase: ManageHabitUseCase

    @Autowired
    private lateinit var logHabitExecutionUseCase: LogHabitExecutionUseCase

    @Autowired
    private lateinit var syncHealthHabitEventUseCase: SyncHealthHabitEventUseCase

    @Autowired
    private lateinit var manageTodoCategoryUseCase: ManageTodoCategoryUseCase

    @Autowired
    private lateinit var manageTodoItemUseCase: ManageTodoItemUseCase

    @Autowired
    private lateinit var completeTodoItemUseCase: CompleteTodoItemUseCase

    @Autowired
    private lateinit var getDailyRoutineViewUseCase: GetDailyRoutineViewUseCase

    @Autowired
    private lateinit var getExpectedVsAchievedUseCase: GetExpectedVsAchievedUseCase

    @Autowired
    private lateinit var getHabitHeatmapUseCase: GetHabitHeatmapUseCase

    @Autowired
    private lateinit var getRoutineSummaryForAiUseCase: GetRoutineSummaryForAiUseCase

    @Autowired
    private lateinit var manageUserRoutineSettingsUseCase: ManageUserRoutineSettingsUseCase

    @Test
    fun `fluxo completo ponta a ponta de Habitos, To-Do List, Auto-Check Health e Analytics`() {
        val userId = "test-e2e-user"
        val today = LocalDate.now()

        // 1. Configurar inicio da semana para MONDAY
        val settings = manageUserRoutineSettingsUseCase.updateSettings(
            userId = userId,
            weekStartDay = DayOfWeek.MONDAY,
            timezone = "America/Sao_Paulo",
            eveningReminderEnabled = true,
            eveningReminderTime = "20:00"
        )
        settings.weekStartDay shouldBe DayOfWeek.MONDAY

        // 2. Criar Categoria de To-Do ("Casa")
        val catCasa = manageTodoCategoryUseCase.createCategory(
            userId = userId,
            name = "Casa",
            colorHex = "#8B5CF6",
            icon = "home"
        )

        // 3. Criar Habito Mensal Flexivel vinculado a To-Do ("Executar 2 itens de To-Do List de Casa por mes")
        val todoHabit = manageHabitUseCase.createHabit(
            userId = userId,
            title = "Executar 2 itens de Casa no mês",
            description = "Concluir 2 tarefas de Casa",
            icon = "home",
            colorHex = "#8B5CF6",
            scheduleType = ScheduleType.FLEXIBLE_MONTHLY,
            daysOfWeek = null,
            daysOfMonth = null,
            targetCountPerPeriod = 2,
            quotaCountMode = QuotaCountMode.TOTAL_EXECUTIONS,
            completionMode = CompletionMode.TODO_LINKED,
            dailyTargetValue = 1.0,
            unit = "tarefas",
            linkedTodoCategoryId = catCasa.id,
            healthTriggerType = null,
            healthExerciseType = null,
            healthMinDurationMinutes = null,
            startDate = today.withDayOfMonth(1)
        )

        // 4. Criar Habito Semanal Flexivel Health Auto ("Treinar 3x na semana")
        val healthHabit = manageHabitUseCase.createHabit(
            userId = userId,
            title = "Treinar 3x na semana",
            description = "Qualquer treino acima de 20 min",
            icon = "dumbbell",
            colorHex = "#10B981",
            scheduleType = ScheduleType.FLEXIBLE_WEEKLY,
            daysOfWeek = null,
            daysOfMonth = null,
            targetCountPerPeriod = 3,
            quotaCountMode = QuotaCountMode.DAYS_COMPLETED,
            completionMode = CompletionMode.HEALTH_AUTO,
            dailyTargetValue = 1.0,
            unit = "treinos",
            linkedTodoCategoryId = null,
            healthTriggerType = HealthTriggerType.WORKOUT_SESSION,
            healthExerciseType = "ANY",
            healthMinDurationMinutes = 20,
            startDate = today.minusDays(7)
        )

        // 5. Criar 2 Tarefas na Categoria "Casa" e concluir ambas
        val todo1 = manageTodoItemUseCase.createTodo(
            userId = userId,
            title = "Trocar lâmpada da sala",
            description = null,
            categoryId = catCasa.id,
            linkedHabitId = null,
            priority = TodoPriority.HIGH,
            dueDate = today,
            subtaskTitles = listOf("Comprar lâmpada LED", "Instalar")
        )
        val todo2 = manageTodoItemUseCase.createTodo(
            userId = userId,
            title = "Arrumar gaveta da cozinha",
            description = null,
            categoryId = catCasa.id,
            linkedHabitId = null,
            priority = TodoPriority.MEDIUM,
            dueDate = today,
            subtaskTitles = null
        )

        // Concluir subtarefa nao deve concluir habito
        manageTodoItemUseCase.toggleSubtask(userId, todo1.todo.id, todo1.todo.subtasks.first().id)
        manageHabitUseCase.getHabitById(userId, todoHabit.habit.id).periodAchievedCount shouldBe 0

        // Concluir as 2 tarefas principais -> habito mensal atinge 2/2!
        completeTodoItemUseCase.toggleTodoStatus(userId, todo1.todo.id, today)
        completeTodoItemUseCase.toggleTodoStatus(userId, todo2.todo.id, today)
        manageHabitUseCase.getHabitById(userId, todoHabit.habit.id).periodAchievedCount shouldBe 2

        // Reabrir todo2 -> estorna automaticamente para 1/2!
        val reopened = completeTodoItemUseCase.toggleTodoStatus(userId, todo2.todo.id, today)
        reopened.todo.status shouldBe TodoStatus.PENDING
        manageHabitUseCase.getHabitById(userId, todoHabit.habit.id).periodAchievedCount shouldBe 1

        // Concluir novamente todo2 -> volta para 2/2!
        completeTodoItemUseCase.toggleTodoStatus(userId, todo2.todo.id, today)
        manageHabitUseCase.getHabitById(userId, todoHabit.habit.id).periodAchievedCount shouldBe 2

        // 6. Simular evento de treino vindo do health_service
        syncHealthHabitEventUseCase.processHealthEvent(
            HealthEventPayload(
                userId = userId,
                eventType = HealthTriggerType.WORKOUT_SESSION,
                date = today,
                value = 1.0,
                durationMinutes = 45,
                exerciseType = "RUNNING",
                referenceId = "workout-123"
            )
        )
        manageHabitUseCase.getHabitById(userId, healthHabit.habit.id).completedToday shouldBe true

        // 7. Consultar Visao do Dia, Comparativo Mensal, Heatmap e Summary IA
        val dailyView = getDailyRoutineViewUseCase.execute(userId, today)
        dailyView.flexibleHabitsInPeriod.size shouldBe 2

        val monthlyComparison = getExpectedVsAchievedUseCase.execute(userId, PeriodType.MONTH, today)
        val breakdownCasa = monthlyComparison.habitBreakdown.first { it.habitId == todoHabit.habit.id }
        breakdownCasa.expectedInPeriod shouldBe 2
        breakdownCasa.achievedRawInPeriod shouldBe 2
        breakdownCasa.percentageCapped shouldBe 100.0

        val heatmap = getHabitHeatmapUseCase.execute(userId, months = 3)
        (heatmap.totalActiveDays >= 1) shouldBe true

        val aiSummary = getRoutineSummaryForAiUseCase.execute(userId, today)
        aiSummary.date shouldBe today
    }
}
