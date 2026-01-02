package com.oleksiy.quicktodo.ui.util

import com.oleksiy.quicktodo.model.CodeLocation
import com.oleksiy.quicktodo.model.Task
import com.oleksiy.quicktodo.ui.ChecklistConstants
import com.oleksiy.quicktodo.ui.TaskTree
import com.oleksiy.quicktodo.ui.TaskTreeCellRenderer
import com.intellij.ui.CheckedTreeNode
import java.awt.event.MouseEvent

/**
 * Utility for detecting and handling code location link interactions in the task tree.
 * Eliminates duplicated geometry calculation logic for link detection.
 */
object LocationLinkDetector {

    /**
     * Data class holding the result of link bounds calculation.
     */
    data class LinkBounds(
        val startX: Int,
        val endX: Int,
        val task: Task
    )

    /**
     * Calculates the link bounds for a location link at the given mouse position.
     * Returns null if there's no link at that position or the task has no code location.
     */
    fun getLinkBoundsAt(
        tree: TaskTree,
        renderer: TaskTreeCellRenderer,
        event: MouseEvent
    ): LinkBounds? {
        val path = tree.getPathForRowAt(event.x, event.y) ?: return null
        val node = path.lastPathComponent as? CheckedTreeNode ?: return null
        val task = node.userObject as? Task ?: return null

        if (!task.hasCodeLocation()) return null

        val row = tree.getRowForPath(path)
        val rowBounds = tree.getRowBounds(row) ?: return null

        // Configure renderer for this cell to get correct bounds
        tree.cellRenderer.getTreeCellRendererComponent(
            tree, node, tree.isRowSelected(row), tree.isExpanded(row),
            tree.model.isLeaf(node), row, tree.hasFocus()
        )

        if (renderer.linkText.isEmpty()) return null

        // Calculate text start position (after checkbox)
        val textStartX = rowBounds.x + ChecklistConstants.CHECKBOX_WIDTH + 4

        // Use font metrics for accurate positioning
        val fm = tree.getFontMetrics(tree.font)
        val locationStartX = textStartX + fm.stringWidth(renderer.textBeforeLink)
        val locationEndX = locationStartX + fm.stringWidth(renderer.linkText)

        return LinkBounds(locationStartX, locationEndX, task)
    }

    /**
     * Checks if the mouse is currently over a location link.
     */
    fun isMouseOverLink(
        tree: TaskTree,
        renderer: TaskTreeCellRenderer,
        event: MouseEvent
    ): Boolean {
        val bounds = getLinkBoundsAt(tree, renderer, event) ?: return false
        return event.x >= bounds.startX && event.x <= bounds.endX
    }

    /**
     * Returns the code location if the click was on a location link, null otherwise.
     */
    fun getClickedLocation(
        tree: TaskTree,
        renderer: TaskTreeCellRenderer,
        event: MouseEvent
    ): CodeLocation? {
        val bounds = getLinkBoundsAt(tree, renderer, event) ?: return null
        if (event.x >= bounds.startX && event.x <= bounds.endX) {
            return bounds.task.codeLocation
        }
        return null
    }
}
