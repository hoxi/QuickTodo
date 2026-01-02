package com.oleksiy.quicktodo.ui.handler

import com.oleksiy.quicktodo.model.Task
import com.oleksiy.quicktodo.service.FocusService
import com.oleksiy.quicktodo.service.TaskService

/**
 * Handles removing tasks.
 */
class RemoveTaskHandler(
    private val taskService: TaskService,
    private val focusService: FocusService,
    private val getSelectedTasks: () -> List<Task>
) {
    /**
     * Removes all currently selected tasks.
     */
    fun removeSelectedTasks() {
        val selectedTasks = getSelectedTasks()
        if (selectedTasks.isEmpty()) return

        // Stop focus timer for all deleted tasks
        selectedTasks.forEach { focusService.onTaskDeleted(it.id) }

        if (selectedTasks.size == 1) {
            taskService.removeTask(selectedTasks.first().id)
        } else {
            taskService.removeTasks(selectedTasks.map { it.id })
        }
    }
}
