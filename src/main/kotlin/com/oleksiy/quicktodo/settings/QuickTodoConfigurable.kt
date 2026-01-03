package com.oleksiy.quicktodo.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.TitledSeparator
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import javax.swing.ButtonGroup
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JRadioButton

/**
 * Settings page for QuickTodo plugin under Tools > QuickTodo.
 */
class QuickTodoConfigurable : Configurable {
    private var settingsPanel: JPanel? = null
    private val radioButtons = mutableMapOf<TooltipBehavior, JRadioButton>()

    override fun getDisplayName(): String = "QuickTodo"

    override fun createComponent(): JComponent {
        val settings = QuickTodoSettings.getInstance()

        // Create radio buttons for tooltip behavior
        val buttonGroup = ButtonGroup()
        for (behavior in TooltipBehavior.values()) {
            val radioButton = JRadioButton(behavior.displayName)
            radioButton.isSelected = settings.getTooltipBehavior() == behavior
            buttonGroup.add(radioButton)
            radioButtons[behavior] = radioButton
        }

        settingsPanel = FormBuilder.createFormBuilder()
            .addComponent(TitledSeparator("Tooltip Behavior"))
            .addComponent(radioButtons[TooltipBehavior.ALWAYS]!!, 1)
            .addComponent(radioButtons[TooltipBehavior.TRUNCATED]!!, 1)
            .addComponent(radioButtons[TooltipBehavior.NEVER]!!, 1)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return settingsPanel!!
    }

    override fun isModified(): Boolean {
        val settings = QuickTodoSettings.getInstance()
        val currentBehavior = settings.getTooltipBehavior()

        return radioButtons.entries.any { (behavior, button) ->
            button.isSelected && behavior != currentBehavior
        }
    }

    override fun apply() {
        val settings = QuickTodoSettings.getInstance()
        radioButtons.forEach { (behavior, button) ->
            if (button.isSelected) {
                settings.setTooltipBehavior(behavior)
            }
        }
    }

    override fun reset() {
        val settings = QuickTodoSettings.getInstance()
        val currentBehavior = settings.getTooltipBehavior()

        radioButtons.forEach { (behavior, button) ->
            button.isSelected = behavior == currentBehavior
        }
    }
}
