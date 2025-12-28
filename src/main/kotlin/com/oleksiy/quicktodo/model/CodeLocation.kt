package com.oleksiy.quicktodo.model

import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag

/**
 * Represents a location in code (file path + cursor position or selection range).
 * Uses relative path for project portability.
 */
@Tag("codeLocation")
data class CodeLocation(
    @Attribute("relativePath")
    var relativePath: String = "",

    @Attribute("line")
    var line: Int = 0,  // 0-based line number (start of selection or cursor)

    @Attribute("column")
    var column: Int = 0,  // 0-based column number (start of selection or cursor)

    @Attribute("endLine")
    var endLine: Int = -1,  // 0-based end line (-1 means no selection)

    @Attribute("endColumn")
    var endColumn: Int = -1  // 0-based end column (-1 means no selection)
) {
    constructor() : this("", 0, 0, -1, -1)

    /**
     * Returns true if this location represents a selection range (not just cursor position)
     */
    fun hasSelection(): Boolean = endLine >= 0 && endColumn >= 0 &&
            (endLine > line || (endLine == line && endColumn > column))

    /**
     * Returns display string like "TaskService.kt:42" or "TaskService.kt:42-48" for multi-line selection
     */
    fun toDisplayString(): String {
        val fileName = relativePath.substringAfterLast('/')
        return if (hasSelection() && endLine > line) {
            // Multi-line selection: show line range
            "$fileName:${line + 1}-${endLine + 1}"
        } else {
            // Single line (cursor or single-line selection)
            "$fileName:${line + 1}"
        }
    }

    fun isValid(): Boolean = relativePath.isNotBlank()
}
