package com.oleksiy.quicktodo.ui

import com.oleksiy.quicktodo.model.CodeLocation
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

/**
 * Utility for capturing and navigating to code locations.
 */
object CodeLocationUtil {

    /**
     * Captures the current cursor position or selection from the active editor.
     * @return CodeLocation with relative path and position/selection, or null if no editor is open
     */
    fun captureCurrentLocation(project: Project): CodeLocation? {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
        val virtualFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull() ?: return null

        val basePath = project.basePath ?: return null
        val absolutePath = virtualFile.path

        // Convert to relative path from project root
        val relativePath = if (absolutePath.startsWith(basePath)) {
            absolutePath.removePrefix(basePath).removePrefix("/")
        } else {
            // File is outside project - still use relative path from project root
            virtualFile.name
        }

        val selectionModel = editor.selectionModel
        val caretModel = editor.caretModel

        return if (selectionModel.hasSelection()) {
            // Capture selection range
            val startPosition = editor.offsetToLogicalPosition(selectionModel.selectionStart)
            val endPosition = editor.offsetToLogicalPosition(selectionModel.selectionEnd)

            CodeLocation(
                relativePath = relativePath,
                line = startPosition.line,
                column = startPosition.column,
                endLine = endPosition.line,
                endColumn = endPosition.column
            )
        } else {
            // Capture cursor position only
            val logicalPosition = caretModel.logicalPosition

            CodeLocation(
                relativePath = relativePath,
                line = logicalPosition.line,
                column = logicalPosition.column
            )
        }
    }

    /**
     * Navigates to the specified code location and restores selection if present.
     * @return true if navigation succeeded
     */
    fun navigateToLocation(project: Project, location: CodeLocation): Boolean {
        val basePath = project.basePath ?: return false

        // Resolve path (relative or absolute)
        val absolutePath = if (location.relativePath.startsWith("/")) {
            location.relativePath
        } else {
            File(basePath, location.relativePath).absolutePath
        }

        val virtualFile = LocalFileSystem.getInstance().findFileByPath(absolutePath) ?: return false

        val descriptor = OpenFileDescriptor(
            project,
            virtualFile,
            location.line,
            location.column
        )

        val navigated = descriptor.navigateInEditor(project, true)

        // If navigation succeeded and there's a selection, restore it
        if (navigated && location.hasSelection()) {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor
            if (editor != null) {
                val startOffset = editor.logicalPositionToOffset(
                    com.intellij.openapi.editor.LogicalPosition(location.line, location.column)
                )
                val endOffset = editor.logicalPositionToOffset(
                    com.intellij.openapi.editor.LogicalPosition(location.endLine, location.endColumn)
                )
                editor.selectionModel.setSelection(startOffset, endOffset)
            }
        }

        return navigated
    }
}
