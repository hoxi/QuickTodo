package com.oleksiy.quicktodo.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.oleksiy.quicktodo.ui.RecentTasksPopup

class ShowRecentTasksAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        RecentTasksPopup(project).show()
    }
}
