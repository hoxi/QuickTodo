package com.oleksiy.quicktodo.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.oleksiy.quicktodo.settings.QuickTodoSettings",
    storages = [Storage("quicktodo.settings.xml")]
)
@Service(Service.Level.APP)
class QuickTodoSettings : PersistentStateComponent<QuickTodoSettings.State> {

    class State {
        var tooltipBehavior: TooltipBehavior = TooltipBehavior.TRUNCATED
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    fun getTooltipBehavior(): TooltipBehavior = myState.tooltipBehavior

    fun setTooltipBehavior(behavior: TooltipBehavior) {
        myState.tooltipBehavior = behavior
    }

    companion object {
        fun getInstance(): QuickTodoSettings {
            return ApplicationManager.getApplication().getService(QuickTodoSettings::class.java)
        }
    }
}
