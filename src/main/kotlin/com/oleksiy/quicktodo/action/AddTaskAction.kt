package com.oleksiy.quicktodo.action

import com.oleksiy.quicktodo.service.TaskService
import com.oleksiy.quicktodo.ui.NewTaskDialog
import com.oleksiy.quicktodo.model.CodeLocation
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware

class AddTaskAction : AnAction(), DumbAware {
    
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val taskService = project.getService(TaskService::class.java) ?: return

        // Check if we're in an editor context to pre-fill location
        val codeLocation = getEditorLocation(e)
        
        val dialog = if (codeLocation != null) {
            NewTaskDialog(project, codeLocation)
        } else {
            NewTaskDialog(project)
        }
        
        if (dialog.showAndGet()) {
            val text = dialog.getTaskText()
            val priority = dialog.getSelectedPriority()
            val location = dialog.getCodeLocation()
            if (text.isNotBlank()) {
                val task = taskService.addTask(text, priority)
                location?.let { taskService.setTaskLocation(task.id, it) }
            }
        }
    }
    
    private fun getEditorLocation(e: AnActionEvent): CodeLocation? {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return null
        
        val selectionModel = editor.selectionModel
        val caretModel = editor.caretModel
        
        return if (selectionModel.hasSelection()) {
            // Use selection range
            val startLine = editor.document.getLineNumber(selectionModel.selectionStart)
            val endLine = editor.document.getLineNumber(selectionModel.selectionEnd)
            val startColumn = selectionModel.selectionStart - editor.document.getLineStartOffset(startLine)
            val endColumn = selectionModel.selectionEnd - editor.document.getLineStartOffset(endLine)
            CodeLocation(psiFile.virtualFile.path, startLine, startColumn, endLine, endColumn)
        } else {
            // Use current line
            val currentLine = editor.document.getLineNumber(caretModel.offset)
            val currentColumn = caretModel.offset - editor.document.getLineStartOffset(currentLine)
            CodeLocation(psiFile.virtualFile.path, currentLine, currentColumn, -1, -1)
        }
    }
}
