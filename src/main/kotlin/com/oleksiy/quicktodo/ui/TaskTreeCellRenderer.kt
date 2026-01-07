package com.oleksiy.quicktodo.ui

import com.oleksiy.quicktodo.model.Priority
import com.oleksiy.quicktodo.model.Task
import com.oleksiy.quicktodo.service.FocusService
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.SwingConstants
import javax.swing.tree.TreeCellRenderer

/**
 * Custom cell renderer for TaskTree.
 * Renders each task with a checkbox, text, progress indicator, timer, priority icon, and code location link.
 */
class TaskTreeCellRenderer(
    private val focusService: FocusService
) : JPanel(BorderLayout()), TreeCellRenderer {

    private val checkbox = JCheckBox()
    private val textRenderer = SimpleColoredComponent()
    private val timerLabel = JLabel()

    // Track location link text for click detection
    var textBeforeLink: String = ""
        private set
    var linkText: String = ""
        private set

    // Track hovered row for highlight
    var hoveredRow: Int = -1

    init {
        isOpaque = false
        checkbox.isOpaque = false
        textRenderer.isOpaque = false
        timerLabel.isOpaque = false
        timerLabel.horizontalTextPosition = SwingConstants.LEFT

        val centerPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        centerPanel.isOpaque = false
        centerPanel.add(textRenderer)
        centerPanel.add(timerLabel)

        add(checkbox, BorderLayout.WEST)
        add(centerPanel, BorderLayout.CENTER)
    }

    override fun getTreeCellRendererComponent(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        // Reset state
        textRenderer.clear()
        textRenderer.icon = null
        textBeforeLink = ""
        linkText = ""
        timerLabel.text = ""
        timerLabel.icon = null
        timerLabel.isVisible = false

        val node = value as? CheckedTreeNode
        val task = node?.userObject as? Task

        if (task == null) {
            // Root node or non-task node
            textRenderer.append(node?.userObject?.toString() ?: "Tasks")
            checkbox.isVisible = false
            background = null
            return this
        }

        checkbox.isVisible = true

        // Set background based on selection/hover state
        background = when {
            selected -> UIUtil.getTreeSelectionBackground(hasFocus)
            row == hoveredRow -> JBUI.CurrentTheme.List.Hover.background(true)
            else -> null
        }
        isOpaque = selected || row == hoveredRow

        // Set checkbox state from node
        checkbox.isSelected = node.isChecked

        // Determine text style based on task state
        val isFocused = focusService.isFocused(task.id)
        val isAncestorOfFocused = isAncestorOfFocusedTask(task)
        val hasAccumulatedTime = focusService.hasAccumulatedTime(task.id)
        val isCompleted = node.isChecked

        val textAttributes = when {
            isCompleted -> SimpleTextAttributes(
                SimpleTextAttributes.STYLE_STRIKEOUT,
                if (selected) UIUtil.getTreeSelectionForeground(hasFocus) else SimpleTextAttributes.GRAYED_ATTRIBUTES.fgColor
            )
            isFocused || isAncestorOfFocused -> SimpleTextAttributes(
                SimpleTextAttributes.STYLE_BOLD or SimpleTextAttributes.STYLE_UNDERLINE,
                if (selected) UIUtil.getTreeSelectionForeground(hasFocus) else null
            )
            selected -> SimpleTextAttributes(
                SimpleTextAttributes.STYLE_PLAIN,
                UIUtil.getTreeSelectionForeground(hasFocus)
            )
            else -> SimpleTextAttributes.REGULAR_ATTRIBUTES
        }

        // Render task text
        textRenderer.append(task.text, textAttributes)

        // Show completion progress for parent tasks (e.g., "2/5")
        if (task.subtasks.isNotEmpty()) {
            val (completed, total) = countCompletionProgress(task)
            textRenderer.append("  ")
            val progressAttributes = if (selected) {
                SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, UIUtil.getTreeSelectionForeground(hasFocus))
            } else {
                SimpleTextAttributes.GRAYED_ATTRIBUTES
            }
            textRenderer.append(
                "$completed/$total",
                progressAttributes
            )
        }

        // Show timer if has accumulated time
        if (hasAccumulatedTime) {
            val timeStr = focusService.getFormattedTime(task.id)
            timerLabel.text = " $timeStr"
            timerLabel.icon = QuickTodoIcons.Timer
            timerLabel.foreground = if (selected) {
                UIUtil.getTreeSelectionForeground(hasFocus)
            } else {
                SimpleTextAttributes.GRAYED_ATTRIBUTES.fgColor
            }
            timerLabel.isVisible = true
        }

        // Set flag icon based on priority (on the left, before text)
        val priority = task.getPriorityEnum()
        if (priority != Priority.NONE) {
            val icon = QuickTodoIcons.getIconForPriority(priority)
            if (icon != null) {
                textRenderer.icon = icon
            }
        }

        // Show linked file location at the end
        if (task.hasCodeLocation()) {
            textRenderer.append("        ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
            textBeforeLink = textRenderer.toString()
            linkText = task.codeLocation!!.toDisplayString()
            val linkAttributes = if (selected) {
                SimpleTextAttributes(
                    SimpleTextAttributes.STYLE_UNDERLINE,
                    UIUtil.getTreeSelectionForeground(hasFocus)
                )
            } else {
                SimpleTextAttributes(
                    SimpleTextAttributes.STYLE_UNDERLINE,
                    SimpleTextAttributes.GRAYED_ATTRIBUTES.fgColor
                )
            }
            textRenderer.append(linkText, linkAttributes)
        }

        return this
    }

    private fun countCompletionProgress(task: Task): Pair<Int, Int> {
        var completed = 0
        var total = 0
        for (subtask in task.subtasks) {
            total++
            if (subtask.isCompleted) {
                completed++
            }
            val (nestedCompleted, nestedTotal) = countCompletionProgress(subtask)
            completed += nestedCompleted
            total += nestedTotal
        }
        return Pair(completed, total)
    }

    private fun isAncestorOfFocusedTask(task: Task): Boolean {
        val focusedTaskId = focusService.getFocusedTaskId() ?: return false
        return task.id != focusedTaskId && task.findTask(focusedTaskId) != null
    }
}
