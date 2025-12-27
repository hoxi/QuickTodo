package com.example.quicktodo.action

import com.example.quicktodo.service.TaskService
import com.example.quicktodo.ui.NewTaskDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class AddTaskAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val taskService = project.getService(TaskService::class.java) ?: return

        val dialog = NewTaskDialog(project)
        if (dialog.showAndGet()) {
            val text = dialog.getTaskText()
            val priority = dialog.getSelectedPriority()
            if (text.isNotBlank()) {
                taskService.addTask(text, priority)
            }
        }
    }
}
