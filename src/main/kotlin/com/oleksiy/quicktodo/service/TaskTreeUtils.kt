package com.oleksiy.quicktodo.service

import com.oleksiy.quicktodo.model.Task

/**
 * Pure extension functions for tree traversal operations on task lists.
 * No state, no side effects - just tree navigation utilities.
 */

/**
 * Finds a task by ID in this list or any nested subtasks.
 */
fun List<Task>.findTaskById(taskId: String): Task? {
    for (task in this) {
        task.findTask(taskId)?.let { return it }
    }
    return null
}

/**
 * Finds the parent ID of a task within the task hierarchy.
 * Returns null if the task is at root level or not found.
 */
fun findParentId(tasks: List<Task>, taskId: String): String? {
    return findParentIdRecursive(tasks, taskId, null)
}

private fun findParentIdRecursive(
    tasks: List<Task>,
    targetId: String,
    currentParentId: String?
): String? {
    for (task in tasks) {
        if (task.id == targetId) return currentParentId
        val result = findParentIdRecursive(task.subtasks, targetId, task.id)
        if (result != null) return result
    }
    return null
}

/**
 * Checks if ancestorId is an ancestor of descendantId in the task hierarchy.
 */
fun isAncestorOf(tasks: List<Task>, ancestorId: String, descendantId: String): Boolean {
    val ancestor = tasks.findTaskById(ancestorId) ?: return false
    return ancestor.findTask(descendantId) != null
}

/**
 * Gets the index of a task within its sibling list.
 * If parentId is null, searches in the root task list.
 */
internal fun getTaskIndex(tasks: List<Task>, taskId: String, parentId: String?): Int {
    val siblings = if (parentId == null) {
        tasks
    } else {
        tasks.findTaskById(parentId)?.subtasks ?: return -1
    }
    return siblings.indexOfFirst { it.id == taskId }
}
