@file:Suppress("UnstableApiUsage")

package ai.kilocode.client

import ai.kilocode.rpc.KiloProjectRpcApi
import ai.kilocode.rpc.dto.KiloWorkspaceStateDto
import ai.kilocode.rpc.dto.KiloWorkspaceStatusDto
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import fleet.rpc.client.durable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Project-level frontend service that provides reactive access
 * to project-scoped data (providers, agents, commands, skills)
 * and resolves the real project directory from the backend.
 *
 * In split mode, [Project.getBasePath] returns a synthetic sandbox
 * path. This service resolves the backend's actual project directory
 * via [KiloProjectRpcApi.directory] and uses it for all CLI calls.
 */
@Service(Service.Level.PROJECT)
class KiloProjectService(
    private val project: Project,
    private val cs: CoroutineScope,
) {
    companion object {
        private val LOG = Logger.getInstance(KiloProjectService::class.java)
        private val init = KiloWorkspaceStateDto(KiloWorkspaceStatusDto.PENDING)
    }

    private val hint: String get() = project.basePath ?: ""

    private val _directory = MutableStateFlow("")

    /** The real project directory as resolved by the backend. */
    val directory: StateFlow<String> = _directory.asStateFlow()

    init {
        cs.launch {
            try {
                val resolved = durable { KiloProjectRpcApi.getInstance().directory(hint) }
                LOG.info("Resolved project directory: hint=$hint → resolved=$resolved")
                _directory.value = resolved
            } catch (e: Exception) {
                LOG.warn("Failed to resolve project directory, falling back to hint=$hint", e)
                _directory.value = hint
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state: StateFlow<KiloWorkspaceStateDto> = _directory
        .flatMapLatest { dir ->
            if (dir.isEmpty()) return@flatMapLatest flowOf(init)
            flow {
                durable {
                    KiloProjectRpcApi.getInstance()
                        .state(dir)
                        .collect { emit(it) }
                }
            }
        }
        .stateIn(cs, SharingStarted.Eagerly, init)

    /** Trigger a full reload of all project data. */
    fun reload() {
        cs.launch {
            val dir = _directory.value
            if (dir.isEmpty()) return@launch
            try {
                durable { KiloProjectRpcApi.getInstance().reload(dir) }
            } catch (e: Exception) {
                LOG.warn("project data reload failed", e)
            }
        }
    }
}
