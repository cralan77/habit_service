package com.habit.domain.service

import com.habit.domain.model.CompletionMode
import com.habit.domain.model.Habit
import com.habit.domain.model.HabitExecutionLog
import com.habit.domain.model.HabitLogSource
import com.habit.domain.model.PeriodType
import com.habit.domain.model.QuotaCountMode
import com.habit.domain.model.ScheduleType
import com.habit.domain.model.TodoItem
import com.habit.domain.model.TodoStatus
import com.habit.domain.model.TodoSubtask
import com.habit.domain.repository.HabitExecutionLogRepository
import com.habit.domain.repository.HabitRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

class TodoHabitSyncDomainServiceTest {

    private lateinit var inMemoryHabits: MutableMap<UUID, Habit>
    private lateinit var inMemoryLogs: MutableList<HabitExecutionLog>
    private lateinit var habitRepository: HabitRepository
    private lateinit var logRepository: HabitExecutionLogRepository
    private lateinit var syncService: TodoHabitSyncDomainService
    private val scheduleEvaluator = HabitScheduleEvaluatorService()
    private val streakCalculator = HabitStreakCalculatorService(scheduleEvaluator)
    private val analyticsEngine = HabitAnalyticsEngineService(scheduleEvaluator, streakCalculator)

    @BeforeEach
    fun setUp() {
        inMemoryHabits = mutableMapOf()
        inMemoryLogs = mutableListOf()

        habitRepository = object : HabitRepository {
            override fun save(habit: Habit): Habit {
                inMemoryHabits[habit.id] = habit
                return habit
            }
            override fun findById(id: UUID): Habit? = inMemoryHabits[id]
            override fun findByUserId(userId: String, includeArchived: Boolean): List<Habit> =
                inMemoryHabits.values.filter { it.userId == userId && (includeArchived || it.isActive) }
            override fun findByLinkedTodoCategoryId(userId: String, categoryId: UUID): List<Habit> =
                inMemoryHabits.values.filter { it.userId == userId && it.linkedTodoCategoryId == categoryId }
            override fun deleteById(id: UUID) {
                inMemoryHabits.remove(id)
            }
        }

        logRepository = object : HabitExecutionLogRepository {
            override fun save(log: HabitExecutionLog): HabitExecutionLog {
                inMemoryLogs.removeIf { it.id == log.id }
                inMemoryLogs.add(log)
                return log
            }
            override fun findByHabitIdAndDate(habitId: UUID, date: LocalDate): List<HabitExecutionLog> =
                inMemoryLogs.filter { it.habitId == habitId && it.executionDate == date }
            override fun findByHabitIdAndDateRange(habitId: UUID, startDate: LocalDate, endDate: LocalDate): List<HabitExecutionLog> =
                inMemoryLogs.filter { it.habitId == habitId && !it.executionDate.isBefore(startDate) && !it.executionDate.isAfter(endDate) }
            override fun findByUserIdAndDateRange(userId: String, startDate: LocalDate, endDate: LocalDate): List<HabitExecutionLog> =
                inMemoryLogs.filter { it.userId == userId && !it.executionDate.isBefore(startDate) && !it.executionDate.isAfter(endDate) }
            override fun findByHabitIdAndSourceAndReferenceId(
                habitId: UUID,
                source: HabitLogSource,
                sourceReferenceId: String
            ): HabitExecutionLog? =
                inMemoryLogs.firstOrNull {
                    it.habitId == habitId && it.source == source && it.sourceReferenceId == sourceReferenceId
                }
            override fun deleteByHabitIdAndDateAndSource(habitId: UUID, date: LocalDate, source: HabitLogSource) {
                inMemoryLogs.removeIf { it.habitId == habitId && it.executionDate == date && it.source == source }
            }
            override fun deleteBySourceAndReferenceId(source: HabitLogSource, sourceReferenceId: String) {
                inMemoryLogs.removeIf { it.source == source && it.sourceReferenceId == sourceReferenceId }
            }
        }

        syncService = TodoHabitSyncDomainService(habitRepository, logRepository)
    }

    @Test
    fun `deve concluir habito Executar 2 itens de To-Do List por mes ao concluir 2 tarefas da categoria vinculada`() {
        val categoryCasaId = UUID.randomUUID()
        val monthlyHabit = Habit(
            userId = "user-1",
            title = "Executar 2 itens de To-Do List de Casa por mês",
            scheduleType = ScheduleType.FLEXIBLE_MONTHLY,
            targetCountPerPeriod = 2,
            quotaCountMode = QuotaCountMode.TOTAL_EXECUTIONS,
            completionMode = CompletionMode.TODO_LINKED,
            linkedTodoCategoryId = categoryCasaId,
            startDate = LocalDate.of(2026, 10, 1)
        )
        habitRepository.save(monthlyHabit)

        val execDate = LocalDate.of(2026, 10, 28)
        val todo1 = TodoItem(
            userId = "user-1",
            title = "Consertar torneira",
            categoryId = categoryCasaId,
            status = TodoStatus.COMPLETED
        )
        val todo2 = TodoItem(
            userId = "user-1",
            title = "Organizar armário",
            categoryId = categoryCasaId,
            status = TodoStatus.COMPLETED
        )

        syncService.syncOnTodoStatusChanged(todo1, execDate)
        syncService.syncOnTodoStatusChanged(todo2, execDate)

        // Mesmo concluídas no mesmo dia, ambas contam para a cota mensal (2/2 = 100% Sucesso!)
        val comparison = analyticsEngine.buildExpectedVsAchievedComparison(
            periodType = PeriodType.MONTH,
            referenceDate = execDate,
            weekStartDay = DayOfWeek.MONDAY,
            habits = listOf(monthlyHabit),
            logs = inMemoryLogs,
            skips = emptyList()
        )

        comparison.overallMetrics.totalExpected shouldBe 2
        comparison.overallMetrics.totalAchievedCapped shouldBe 2
        comparison.overallMetrics.consistencyPercentage shouldBe 100.0
    }

    @Test
    fun `deve deduplicar quando tarefa possui categoryId e linkedHabitId apontando para o mesmo habito`() {
        val categoryId = UUID.randomUUID()
        val habit = Habit(
            userId = "user-1",
            title = "Tarefas de Estudo",
            scheduleType = ScheduleType.FLEXIBLE_WEEKLY,
            targetCountPerPeriod = 3,
            quotaCountMode = QuotaCountMode.TOTAL_EXECUTIONS,
            completionMode = CompletionMode.TODO_LINKED,
            linkedTodoCategoryId = categoryId,
            startDate = LocalDate.of(2026, 10, 1)
        )
        habitRepository.save(habit)

        val todo = TodoItem(
            userId = "user-1",
            title = "Revisar capítulo de Kotlin",
            categoryId = categoryId,
            linkedHabitId = habit.id, // Aponta tanto via categoria quanto via vínculo direto!
            status = TodoStatus.COMPLETED
        )

        val impacted = syncService.syncOnTodoStatusChanged(todo, LocalDate.of(2026, 10, 28))

        impacted.shouldHaveSize(1)
        inMemoryLogs.shouldHaveSize(1)
    }

    @Test
    fun `nao deve incrementar habito ao concluir apenas subtarefas enquanto tarefa pai estiver PENDING`() {
        val categoryId = UUID.randomUUID()
        val habit = Habit(
            userId = "user-1",
            title = "Tarefas de Casa",
            scheduleType = ScheduleType.FLEXIBLE_MONTHLY,
            targetCountPerPeriod = 2,
            quotaCountMode = QuotaCountMode.TOTAL_EXECUTIONS,
            completionMode = CompletionMode.TODO_LINKED,
            linkedTodoCategoryId = categoryId,
            startDate = LocalDate.of(2026, 10, 1)
        )
        habitRepository.save(habit)

        val todoId = UUID.randomUUID()
        val pendingTodoWithCompletedSubtasks = TodoItem(
            id = todoId,
            userId = "user-1",
            title = "Limpeza geral",
            categoryId = categoryId,
            status = TodoStatus.PENDING,
            subtasks = listOf(
                TodoSubtask(todoId = todoId, title = "Varrer sala", isCompleted = true),
                TodoSubtask(todoId = todoId, title = "Limpar vidros", isCompleted = true)
            )
        )

        // Subtarefas não disparam syncOnTodoStatusChanged com COMPLETED
        syncService.syncOnTodoStatusChanged(pendingTodoWithCompletedSubtasks, LocalDate.of(2026, 10, 28))
        inMemoryLogs.shouldHaveSize(0)
    }

    @Test
    fun `deve estornar automaticamente (2 de 2 para 1 de 2) ao reabrir uma tarefa concluida`() {
        val categoryId = UUID.randomUUID()
        val habit = Habit(
            userId = "user-1",
            title = "Executar 2 itens de To-Do List por mês",
            scheduleType = ScheduleType.FLEXIBLE_MONTHLY,
            targetCountPerPeriod = 2,
            quotaCountMode = QuotaCountMode.TOTAL_EXECUTIONS,
            completionMode = CompletionMode.TODO_LINKED,
            linkedTodoCategoryId = categoryId,
            startDate = LocalDate.of(2026, 10, 1)
        )
        habitRepository.save(habit)

        val date = LocalDate.of(2026, 10, 28)
        val todo1 = TodoItem(userId = "user-1", title = "Tarefa 1", categoryId = categoryId, status = TodoStatus.COMPLETED)
        val todo2 = TodoItem(userId = "user-1", title = "Tarefa 2", categoryId = categoryId, status = TodoStatus.COMPLETED)

        syncService.syncOnTodoStatusChanged(todo1, date)
        syncService.syncOnTodoStatusChanged(todo2, date)
        inMemoryLogs.shouldHaveSize(2)

        // Agora o usuário reabre (desmarca) a Tarefa 2 -> status volta para PENDING
        val reopenedTodo2 = todo2.copy(status = TodoStatus.PENDING, completedAt = null)
        syncService.syncOnTodoStatusChanged(reopenedTodo2, date)

        inMemoryLogs.shouldHaveSize(1)
        val achievedAfterReopen = scheduleEvaluator.countAchievedInRange(
            habit = habit,
            logsInRange = inMemoryLogs,
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2026, 10, 31)
        )
        achievedAfterReopen shouldBe 1
    }
}
