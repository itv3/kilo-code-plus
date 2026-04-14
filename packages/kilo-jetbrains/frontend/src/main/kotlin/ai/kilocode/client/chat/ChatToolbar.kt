package ai.kilocode.client.chat

import ai.kilocode.rpc.dto.AgentDto
import ai.kilocode.rpc.dto.AgentsDto
import ai.kilocode.rpc.dto.ModelDto
import ai.kilocode.rpc.dto.ProviderDto
import ai.kilocode.rpc.dto.ProvidersDto
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.FlowLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JPanel

/**
 * Toolbar with mode (agent) and model selection dropdowns.
 *
 * Populated from workspace data (providers, agents). Changes
 * are forwarded via callbacks to the session service for
 * config updates.
 */
class ChatToolbar(
    private val onModeChanged: (String) -> Unit,
    private val onModelChanged: (String, String) -> Unit,
) : JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), JBUI.scale(2))) {

    private val modeLabel = JBLabel("Mode:")
    private val modeCombo = ComboBox<AgentItem>().apply {
        addActionListener {
            val item = selectedItem as? AgentItem ?: return@addActionListener
            if (!updating) onModeChanged(item.name)
        }
    }

    private val modelLabel = JBLabel("Model:")
    private val modelCombo = ComboBox<ModelItem>().apply {
        addActionListener {
            val item = selectedItem as? ModelItem ?: return@addActionListener
            if (!updating) onModelChanged(item.provider, item.id)
        }
    }

    @Volatile
    private var updating = false

    init {
        border = JBUI.Borders.empty(2, 8)
        add(modeLabel)
        add(modeCombo)
        add(modelLabel)
        add(modelCombo)
    }

    fun setAgents(agents: AgentsDto) {
        updating = true
        try {
            val model = DefaultComboBoxModel<AgentItem>()
            for (agent in agents.agents) {
                model.addElement(AgentItem(agent.name, agent.displayName ?: agent.name))
            }
            modeCombo.model = model
            // Select the default agent
            val idx = agents.agents.indexOfFirst { it.name == agents.default }
            if (idx >= 0) modeCombo.selectedIndex = idx
        } finally {
            updating = false
        }
    }

    fun setProviders(providers: ProvidersDto) {
        updating = true
        try {
            val model = DefaultComboBoxModel<ModelItem>()
            for (provider in providers.providers) {
                if (provider.id !in providers.connected) continue
                for ((id, info) in provider.models) {
                    model.addElement(ModelItem(provider.id, id, "${provider.name} / ${info.name}"))
                }
            }
            modelCombo.model = model

            // Select the default model
            val defaults = providers.defaults
            if (defaults.isNotEmpty()) {
                val entry = defaults.entries.firstOrNull()
                if (entry != null) {
                    val idx = (0 until model.size).firstOrNull { i ->
                        val item = model.getElementAt(i)
                        item.provider == entry.key && item.id == entry.value
                    }
                    if (idx != null) modelCombo.selectedIndex = idx
                }
            }
        } finally {
            updating = false
        }
    }
}

private data class AgentItem(val name: String, val display: String) {
    override fun toString() = display
}

private data class ModelItem(val provider: String, val id: String, val display: String) {
    override fun toString() = display
}
