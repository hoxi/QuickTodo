package com.oleksiy.quicktodo.service

import com.oleksiy.quicktodo.model.Task
import com.oleksiy.quicktodo.undo.ClearCompletedTasksCommand
import com.oleksiy.quicktodo.undo.TaskSnapshot

/**
 * Internal repository for task list mutations.
 * Owned by TaskService; not exposed as a separate IntelliJ service.
 *
 * Handles low-level operations on the task tree without undo recording
 * or listener notification - those are TaskService's responsibility.
 */
internal class TaskRepository(
    private val tasksProvider: () -> MutableList<Task>
) {
    private val tasks: MutableList<Task> get() = tasksProvider()

    /**
     * Removes a task by ID from anywhere in the tree.
     */
    fun removeTask(taskId: String): Boolean {
        return removeTaskFromList(tasks, taskId)
    }

    /**
     * Moves multiple tasks to a new location in the tree.
     */
    fun moveTasks(
        taskIds: List<String>,
        targetParentId: String?,
        targetIndex: Int
    ): Boolean {
        if (taskIds.isEmpty()) return false

        // Collect all tasks to move (in order)
        val tasksToMove = taskIds.mapNotNull { tasks.findTaskById(it) }
        if (tasksToMove.size != taskIds.size) return false

        // Calculate index adjustment for tasks being removed from same parent before target
        val indexAdjustment = calculateIndexAdjustment(taskIds, targetParentId, targetIndex)

        // Remove all tasks from their current locations
        for (taskId in taskIds) {
            if (!removeTask(taskId)) return false
        }

        // Adjust target index to account for removed items
        val adjustedTargetIndex = targetIndex - indexAdjustment

        // Insert at new location
        if (targetParentId == null) {
            // Moving to root level
            var insertIndex = adjustedTargetIndex.coerceIn(0, tasks.size)
            for (task in tasksToMove) {
                task.level = 0
                updateSubtaskLevels(task, 0)
                tasks.add(insertIndex, task)
                insertIndex++
            }
        } else {
            // Moving under a parent
            val targetParent = tasks.findTaskById(targetParentId) ?: return false
            if (!targetParent.canAddSubtask()) return false
            var insertIndex = adjustedTargetIndex.coerceIn(0, targetParent.subtasks.size)
            for (task in tasksToMove) {
                task.level = targetParent.level + 1
                updateSubtaskLevels(task, task.level)
                targetParent.subtasks.add(insertIndex, task)
                insertIndex++
            }
        }

        return true
    }

    /**
     * Captures all completed tasks with their positions for undo.
     */
    fun captureCompletedTasks(
        taskList: List<Task> = tasks,
        parentId: String? = null
    ): List<ClearCompletedTasksCommand.RemovedTaskInfo> {
        val result = mutableListOf<ClearCompletedTasksCommand.RemovedTaskInfo>()

        taskList.forEachIndexed { index, task ->
            if (task.isCompleted) {
                result.add(
                    ClearCompletedTasksCommand.RemovedTaskInfo(
                        TaskSnapshot.deepCopy(task),
                        parentId,
                        index
                    )
                )
            } else {
                result.addAll(captureCompletedTasks(task.subtasks, task.id))
            }
        }

        return result
    }

    /**
     * Removes all completed tasks from the tree.
     * Returns the count of removed tasks (including subtasks).
     */
    fun clearCompleted(): Int {
        return clearCompletedFromList(tasks)
    }

    // ============ Private Helpers ============

    private fun removeTaskFromList(taskList: MutableList<Task>, taskId: String): Boolean {
        val iterator = taskList.iterator()
        while (iterator.hasNext()) {
            val task = iterator.next()
            if (task.id == taskId) {
                iterator.remove()
                return true
            }
            if (removeTaskFromList(task.subtasks, taskId)) {
                return true
            }
        }
        return false
    }

    private fun calculateIndexAdjustment(
        taskIds: List<String>,
        targetParentId: String?,
        targetIndex: Int
    ): Int {
        val targetSiblings = if (targetParentId == null) {
            tasks
        } else {
            tasks.findTaskById(targetParentId)?.subtasks ?: return 0
        }

        // Count how many source tasks are in the same parent AND before the target index
        return taskIds.count { taskId ->
            val index = targetSiblings.indexOfFirst { it.id == taskId }
            index in 0..<targetIndex
        }
    }

    private fun updateSubtaskLevels(task: Task, parentLevel: Int) {
        task.level = parentLevel
        task.subtasks.forEach { subtask ->
            updateSubtaskLevels(subtask, parentLevel + 1)
        }
    }

    private fun clearCompletedFromList(taskList: MutableList<Task>): Int {
        var count = 0
        val iterator = taskList.iterator()
        while (iterator.hasNext()) {
            val task = iterator.next()
            if (task.isCompleted) {
                count += 1 + countAllSubtasks(task)
                iterator.remove()
            } else {
                count += clearCompletedFromList(task.subtasks)
            }
        }
        return count
    }

    private fun countAllSubtasks(task: Task): Int {
        var count = task.subtasks.size
        task.subtasks.forEach { count += countAllSubtasks(it) }
        return count
    }
}
