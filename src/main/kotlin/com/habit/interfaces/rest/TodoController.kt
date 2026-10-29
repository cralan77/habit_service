package com.habit.interfaces.rest

import com.habit.application.todo.CompleteTodoItemUseCase
import com.habit.application.todo.ManageTodoCategoryUseCase
import com.habit.application.todo.ManageTodoItemUseCase
import com.habit.domain.model.TodoPriority
import com.habit.domain.model.TodoStatus
import com.habit.interfaces.rest.dto.CreateOrUpdateTodoCategoryRequest
import com.habit.interfaces.rest.dto.CreateSubtaskRequest
import com.habit.interfaces.rest.dto.CreateTodoRequest
import com.habit.interfaces.rest.dto.TodoCategoryResponse
import com.habit.interfaces.rest.dto.TodoItemResponse
import com.habit.interfaces.rest.dto.UpdateTodoRequest
import com.habit.interfaces.rest.security.HabitUserPrincipal
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
import java.util.UUID

@RestController
@RequestMapping("/api")
class TodoController(
    private val manageTodoCategoryUseCase: ManageTodoCategoryUseCase,
    private val manageTodoItemUseCase: ManageTodoItemUseCase,
    private val completeTodoItemUseCase: CompleteTodoItemUseCase
) {

    @GetMapping("/todo-categories")
    fun listCategories(
        @AuthenticationPrincipal principal: HabitUserPrincipal?
    ): ResponseEntity<List<TodoCategoryResponse>> {
        val userId = principal?.userId ?: "default_mobile_user"
        val categories = manageTodoCategoryUseCase.listCategories(userId)
            .map { TodoCategoryResponse.from(it) }
        return ResponseEntity.ok(categories)
    }

    @PostMapping("/todo-categories")
    fun createCategory(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @RequestBody request: CreateOrUpdateTodoCategoryRequest
    ): ResponseEntity<TodoCategoryResponse> {
        val userId = principal?.userId ?: "default_mobile_user"
        val created = manageTodoCategoryUseCase.createCategory(
            userId = userId,
            name = request.name,
            colorHex = request.colorHex,
            icon = request.icon
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(TodoCategoryResponse.from(created))
    }

    @PutMapping("/todo-categories/{id}")
    fun updateCategory(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @PathVariable id: UUID,
        @RequestBody request: CreateOrUpdateTodoCategoryRequest
    ): ResponseEntity<TodoCategoryResponse> {
        val userId = principal?.userId ?: "default_mobile_user"
        val updated = manageTodoCategoryUseCase.updateCategory(
            userId = userId,
            categoryId = id,
            name = request.name,
            colorHex = request.colorHex,
            icon = request.icon,
            isArchived = request.isArchived
        )
        return ResponseEntity.ok(TodoCategoryResponse.from(updated))
    }

    @DeleteMapping("/todo-categories/{id}")
    fun deleteCategory(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        val userId = principal?.userId ?: "default_mobile_user"
        manageTodoCategoryUseCase.deleteCategory(userId, id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/todos")
    fun listTodos(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) categoryId: UUID?,
        @RequestParam(required = false) priority: TodoPriority?
    ): ResponseEntity<List<TodoItemResponse>> {
        val userId = principal?.userId ?: "default_mobile_user"
        val parsedStatus = when (status?.uppercase()) {
            null, "", "ALL" -> null
            "PENDING" -> TodoStatus.PENDING
            "COMPLETED" -> TodoStatus.COMPLETED
            else -> null
        }
        val todos = manageTodoItemUseCase.listTodos(userId, parsedStatus, categoryId, priority)
            .map { TodoItemResponse.from(it) }
        return ResponseEntity.ok(todos)
    }

    @PostMapping("/todos")
    fun createTodo(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @RequestBody request: CreateTodoRequest
    ): ResponseEntity<TodoItemResponse> {
        val userId = principal?.userId ?: "default_mobile_user"
        val created = manageTodoItemUseCase.createTodo(
            userId = userId,
            title = request.title,
            description = request.description,
            categoryId = request.categoryId,
            linkedHabitId = request.linkedHabitId,
            priority = request.priority,
            dueDate = request.dueDate,
            subtaskTitles = request.subtasks
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(TodoItemResponse.from(created))
    }

    @PutMapping("/todos/{id}")
    fun updateTodo(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @PathVariable id: UUID,
        @RequestBody request: UpdateTodoRequest
    ): ResponseEntity<TodoItemResponse> {
        val userId = principal?.userId ?: "default_mobile_user"
        val updated = manageTodoItemUseCase.updateTodo(
            userId = userId,
            todoId = id,
            title = request.title,
            description = request.description,
            categoryId = request.categoryId,
            linkedHabitId = request.linkedHabitId,
            priority = request.priority,
            dueDate = request.dueDate
        )
        return ResponseEntity.ok(TodoItemResponse.from(updated))
    }

    @PatchMapping("/todos/{id}/toggle")
    fun toggleTodo(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @PathVariable id: UUID
    ): ResponseEntity<TodoItemResponse> {
        val userId = principal?.userId ?: "default_mobile_user"
        val toggled = completeTodoItemUseCase.toggleTodoStatus(userId, id)
        return ResponseEntity.ok(TodoItemResponse.from(toggled))
    }

    @DeleteMapping("/todos/{id}")
    fun deleteTodo(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        val userId = principal?.userId ?: "default_mobile_user"
        manageTodoItemUseCase.deleteTodo(userId, id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/todos/{id}/subtasks")
    fun addSubtask(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @PathVariable id: UUID,
        @RequestBody request: CreateSubtaskRequest
    ): ResponseEntity<TodoItemResponse> {
        val userId = principal?.userId ?: "default_mobile_user"
        val updated = manageTodoItemUseCase.addSubtask(userId, id, request.title)
        return ResponseEntity.status(HttpStatus.CREATED).body(TodoItemResponse.from(updated))
    }

    @PatchMapping("/todos/{id}/subtasks/{subtaskId}/toggle")
    fun toggleSubtask(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @PathVariable id: UUID,
        @PathVariable subtaskId: UUID
    ): ResponseEntity<TodoItemResponse> {
        val userId = principal?.userId ?: "default_mobile_user"
        val updated = manageTodoItemUseCase.toggleSubtask(userId, id, subtaskId)
        return ResponseEntity.ok(TodoItemResponse.from(updated))
    }

    @DeleteMapping("/todos/{id}/subtasks/{subtaskId}")
    fun deleteSubtask(
        @AuthenticationPrincipal principal: HabitUserPrincipal?,
        @PathVariable id: UUID,
        @PathVariable subtaskId: UUID
    ): ResponseEntity<TodoItemResponse> {
        val userId = principal?.userId ?: "default_mobile_user"
        val updated = manageTodoItemUseCase.deleteSubtask(userId, id, subtaskId)
        return ResponseEntity.ok(TodoItemResponse.from(updated))
    }
}
