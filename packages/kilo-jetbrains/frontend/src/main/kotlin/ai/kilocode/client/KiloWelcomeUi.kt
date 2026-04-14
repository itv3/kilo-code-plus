package ai.kilocode.client

import ai.kilocode.client.plugin.KiloBundle
import ai.kilocode.rpc.dto.KiloAppStateDto
import ai.kilocode.rpc.dto.KiloAppStatusDto
import ai.kilocode.rpc.dto.KiloWorkspaceLoadProgressDto
import ai.kilocode.rpc.dto.KiloWorkspaceStateDto
import ai.kilocode.rpc.dto.KiloWorkspaceStatusDto
import ai.kilocode.rpc.dto.LoadErrorDto
import ai.kilocode.rpc.dto.LoadProgressDto
import ai.kilocode.rpc.dto.ProfileStatusDto
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * Welcome panel that shows app initialization progress and workspace
 * loading status. Watches both [KiloAppService] and [KiloProjectService]
 * state flows and renders a unified two-section status view.
 *
 * When connected, all details stay visible — the user always sees
 * what was loaded, the CLI version, and workspace data counts.
 */
class KiloWelcomeUi(
    private val app: KiloAppService,
    private val workspace: KiloProjectService,
    private val cs: CoroutineScope,
) : JPanel(GridBagLayout()), Disposable {

    private val icon = JBLabel(
        IconLoader.getIcon("/icons/kilo-content.svg", KiloWelcomeUi::class.java),
    ).apply {
        horizontalAlignment = SwingConstants.CENTER
        alignmentX = CENTER_ALIGNMENT
    }

    private val status = JBLabel(
        KiloBundle.message("toolwindow.status.disconnected"),
        SwingConstants.CENTER,
    ).apply {
        alignmentX = CENTER_ALIGNMENT
        font = JBUI.Fonts.label(13f)
        foreground = UIUtil.getContextHelpForeground()
        setAllowAutoWrapping(true)
    }

    private val appDetail = JBLabel("", SwingConstants.CENTER).apply {
        alignmentX = CENTER_ALIGNMENT
        font = JBUI.Fonts.smallFont()
        foreground = UIUtil.getContextHelpForeground()
        setAllowAutoWrapping(true)
        setCopyable(true)
    }

    private val wsDetail = JBLabel("", SwingConstants.CENTER).apply {
        alignmentX = CENTER_ALIGNMENT
        font = JBUI.Fonts.smallFont()
        foreground = UIUtil.getContextHelpForeground()
        setAllowAutoWrapping(true)
        setCopyable(true)
    }

    private var appJob: Job? = null
    private var wsJob: Job? = null

    init {
        isOpaque = false

        val body = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(icon)
            add(Box.createVerticalStrut(JBUI.scale(16)))
            add(status)
            add(Box.createVerticalStrut(JBUI.scale(6)))
            add(appDetail)
            add(Box.createVerticalStrut(JBUI.scale(6)))
            add(wsDetail)
        }

        add(body, GridBagConstraints())

        appJob = app.watch { state ->
            update { renderApp(state) }
        }

        wsJob = cs.launch {
            workspace.state.collect { state ->
                update { renderWorkspace(state) }
            }
        }

        app.connect()
    }

    override fun dispose() {
        appJob?.cancel()
        wsJob?.cancel()
        cs.cancel()
    }

    // ------ rendering ------

    private fun renderApp(state: KiloAppStateDto) {
        status.text = title(state)
        val html = appHtml(state)
        appDetail.text = html
        appDetail.isVisible = html.isNotEmpty()
    }

    private fun renderWorkspace(state: KiloWorkspaceStateDto) {
        // Hide workspace section entirely when app isn't ready
        val appReady = app.state.value.status == KiloAppStatusDto.READY
        if (!appReady && state.status == KiloWorkspaceStatusDto.PENDING) {
            wsDetail.isVisible = false
            return
        }
        val html = wsHtml(state)
        wsDetail.text = html
        wsDetail.isVisible = html.isNotEmpty()
    }

    // ------ app formatting ------

    private fun title(state: KiloAppStateDto): String =
        when (state.status) {
            KiloAppStatusDto.DISCONNECTED -> KiloBundle.message("toolwindow.status.disconnected")
            KiloAppStatusDto.CONNECTING -> KiloBundle.message("toolwindow.status.connecting")
            KiloAppStatusDto.LOADING -> KiloBundle.message("toolwindow.status.loading")
            KiloAppStatusDto.READY -> KiloBundle.message("toolwindow.status.connected")
            KiloAppStatusDto.ERROR -> KiloBundle.message(
                "toolwindow.status.error",
                state.error ?: KiloBundle.message("toolwindow.error.unknown"),
            )
        }

    private fun appHtml(state: KiloAppStateDto): String =
        when (state.status) {
            KiloAppStatusDto.LOADING -> appProgress(state.progress)
            KiloAppStatusDto.READY -> appReady(state)
            KiloAppStatusDto.ERROR -> appErrors(state)
            else -> ""
        }

    private fun appProgress(p: LoadProgressDto?): String {
        if (p == null) return ""
        val builder = HtmlBuilder()
            .append(section(KiloBundle.message("toolwindow.section.app")))
            .br()
            .append(item("Config", p.config))
            .br()
            .append(item("Notifications", p.notifications))
            .br()
            .append(profile(p.profile))
        return builder.wrapWithHtmlBody().toString()
    }

    private fun appReady(state: KiloAppStateDto): String {
        val builder = HtmlBuilder()
            .append(section(KiloBundle.message("toolwindow.section.app")))

        // Show final loading state
        val p = state.progress
        if (p != null) {
            builder.br()
                .append(item("Config", p.config))
                .br()
                .append(item("Notifications", p.notifications))
                .br()
                .append(profile(p.profile))
        }

        val ver = app.version
        if (ver != null) {
            builder.br().append(HtmlChunk.text("CLI: $ver"))
        }

        return builder.wrapWithHtmlBody().toString()
    }

    private fun appErrors(state: KiloAppStateDto): String {
        if (state.errors.isEmpty()) return ""
        val builder = HtmlBuilder()
            .append(section(KiloBundle.message("toolwindow.section.app")))
            .br()
            .append(HtmlChunk.text("Failed to load:"))
        state.errors.forEach { err ->
            builder.br().append(formatError(err))
        }
        return builder.wrapWithHtmlBody().toString()
    }

    // ------ workspace formatting ------

    private fun wsHtml(state: KiloWorkspaceStateDto): String =
        when (state.status) {
            KiloWorkspaceStatusDto.PENDING -> wsPending()
            KiloWorkspaceStatusDto.LOADING -> wsProgress(state.progress)
            KiloWorkspaceStatusDto.READY -> wsReady(state)
            KiloWorkspaceStatusDto.ERROR -> wsError(state)
        }

    private fun wsPending(): String {
        val builder = HtmlBuilder()
            .append(section(KiloBundle.message("toolwindow.section.workspace")))
            .br()
            .append(HtmlChunk.text("$DOTS ${KiloBundle.message("toolwindow.workspace.pending")}"))
        return builder.wrapWithHtmlBody().toString()
    }

    private fun wsProgress(p: KiloWorkspaceLoadProgressDto?): String {
        if (p == null) return ""
        val builder = HtmlBuilder()
            .append(section(KiloBundle.message("toolwindow.section.workspace")))
            .br()
            .append(item("Providers", p.providers))
            .br()
            .append(item("Agents", p.agents))
            .br()
            .append(item("Commands", p.commands))
            .br()
            .append(item("Skills", p.skills))
        return builder.wrapWithHtmlBody().toString()
    }

    private fun wsReady(state: KiloWorkspaceStateDto): String {
        val prov = state.providers?.providers?.size ?: 0
        val agents = state.agents?.all?.size ?: 0
        val commands = state.commands.size
        val skills = state.skills.size

        val builder = HtmlBuilder()
            .append(section(KiloBundle.message("toolwindow.section.workspace")))
            .br()
            .append(HtmlChunk.text("$CHECK Providers ($prov)"))
            .br()
            .append(HtmlChunk.text("$CHECK Agents ($agents)"))
            .br()
            .append(HtmlChunk.text("$CHECK Commands ($commands)"))
            .br()
            .append(HtmlChunk.text("$CHECK Skills ($skills)"))
        return builder.wrapWithHtmlBody().toString()
    }

    private fun wsError(state: KiloWorkspaceStateDto): String {
        val msg = state.error ?: "Unknown error"
        val builder = HtmlBuilder()
            .append(section(KiloBundle.message("toolwindow.section.workspace")))
            .br()
            .append(HtmlChunk.text("$CROSS $msg"))
        return builder.wrapWithHtmlBody().toString()
    }

    // ------ shared helpers ------

    private fun section(name: String): HtmlChunk =
        HtmlChunk.text("$DASH $DASH $name $DASH $DASH").bold()

    private fun item(name: String, loaded: Boolean): HtmlChunk =
        HtmlChunk.text(if (loaded) "$CHECK $name" else "$DOTS $name")

    private fun profile(status: ProfileStatusDto): HtmlChunk =
        when (status) {
            ProfileStatusDto.PENDING -> HtmlChunk.text("$DOTS Profile")
            ProfileStatusDto.LOADED -> HtmlChunk.text("$CHECK Profile")
            ProfileStatusDto.NOT_LOGGED_IN -> HtmlChunk.text("$DASH Profile (not logged in)")
        }

    private fun formatError(err: LoadErrorDto): HtmlChunk {
        val suffix = err.detail ?: err.status?.let { "HTTP $it" } ?: ""
        val text = if (suffix.isEmpty()) "$CROSS ${err.resource}"
        else "$CROSS ${err.resource}: $suffix"
        return HtmlChunk.text(text)
    }

    private fun update(block: () -> Unit) {
        ApplicationManager.getApplication().invokeLater(block)
    }

    companion object {
        private const val CHECK = "\u2713"
        private const val CROSS = "\u2717"
        private const val DOTS = "\u2026"
        private const val DASH = "\u2013"
    }
}
