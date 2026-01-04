package com.oleksiy.quicktodo.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import javax.swing.ButtonGroup
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton

/**
 * Settings page for QuickTodo plugin under Tools > QuickTodo.
 */
class QuickTodoConfigurable : Configurable {
    private var settingsPanel: JPanel? = null
    private val radioButtons = mutableMapOf<TooltipBehavior, JRadioButton>()
    private lateinit var autoPauseCheckBox: JBCheckBox
    private lateinit var idleMinutesField: JBTextField

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

        // Create autopause controls on one line
        autoPauseCheckBox = JBCheckBox("Pause if idle")
        autoPauseCheckBox.isSelected = settings.isAutoPauseEnabled()

        idleMinutesField = JBTextField(settings.getIdleMinutes().toString(), 5)

        val autoPausePanel = JPanel().apply {
            layout = java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0)
            add(autoPauseCheckBox)
            add(idleMinutesField)
            add(JLabel("minutes"))
        }

        // Enable/disable idle minutes field based on checkbox
        idleMinutesField.isEnabled = autoPauseCheckBox.isSelected
        autoPauseCheckBox.addActionListener {
            idleMinutesField.isEnabled = autoPauseCheckBox.isSelected
        }

        settingsPanel = FormBuilder.createFormBuilder()
            .addComponent(TitledSeparator("Tooltip Behavior"))
            .addComponent(radioButtons[TooltipBehavior.ALWAYS]!!, 1)
            .addComponent(radioButtons[TooltipBehavior.TRUNCATED]!!, 1)
            .addComponent(radioButtons[TooltipBehavior.NEVER]!!, 1)
            .addVerticalGap(10)
            .addComponent(TitledSeparator("Focus Timer"))
            .addComponent(autoPausePanel, 1)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return settingsPanel!!
    }

    override fun isModified(): Boolean {
        val settings = QuickTodoSettings.getInstance()
        val currentBehavior = settings.getTooltipBehavior()

        val tooltipModified = radioButtons.entries.any { (behavior, button) ->
            button.isSelected && behavior != currentBehavior
        }

        val autoPauseModified = autoPauseCheckBox.isSelected != settings.isAutoPauseEnabled()

        val idleMinutesModified = try {
            idleMinutesField.text.toInt() != settings.getIdleMinutes()
        } catch (e: NumberFormatException) {
            false
        }

        return tooltipModified || autoPauseModified || idleMinutesModified
    }

    override fun apply() {
        val settings = QuickTodoSettings.getInstance()

        radioButtons.forEach { (behavior, button) ->
            if (button.isSelected) {
                settings.setTooltipBehavior(behavior)
            }
        }

        settings.setAutoPauseEnabled(autoPauseCheckBox.isSelected)

        try {
            val minutes = idleMinutesField.text.toInt()
            settings.setIdleMinutes(minutes)
        } catch (e: NumberFormatException) {
            // Keep current value if invalid
        }
    }

    override fun reset() {
        val settings = QuickTodoSettings.getInstance()
        val currentBehavior = settings.getTooltipBehavior()

        radioButtons.forEach { (behavior, button) ->
            button.isSelected = behavior == currentBehavior
        }

        autoPauseCheckBox.isSelected = settings.isAutoPauseEnabled()
        idleMinutesField.text = settings.getIdleMinutes().toString()
        idleMinutesField.isEnabled = autoPauseCheckBox.isSelected
    }
}
