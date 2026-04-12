@file:Suppress("UnstableApiUsage")

package ai.kilocode.backend.rpc

import ai.kilocode.backend.ConnectionState
import ai.kilocode.backend.KiloBackendAppService
import ai.kilocode.rpc.KiloAppRpcApi
import ai.kilocode.rpc.dto.ConnectionStateDto
import ai.kilocode.rpc.dto.ConnectionStatusDto
import ai.kilocode.rpc.dto.HealthDto
import com.intellij.openapi.components.service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Backend implementation of [KiloAppRpcApi].
 *
 * Delegates directly to the app-level [KiloBackendAppService] —
 * no project resolution needed since all operations are app-scoped.
 */
class KiloAppRpcApiImpl : KiloAppRpcApi {

    private val app: KiloBackendAppService get() = service()

    override suspend fun connect() = app.connect()

    override suspend fun state(): Flow<ConnectionStateDto> =
        app.state.map(::dto).distinctUntilChanged()

    override suspend fun health(): HealthDto = app.health()

    override suspend fun restart() = app.restart()

    override suspend fun reinstall() = app.reinstall()

    private fun dto(state: ConnectionState): ConnectionStateDto =
        when (state) {
            ConnectionState.Disconnected -> ConnectionStateDto(ConnectionStatusDto.DISCONNECTED)
            ConnectionState.Connecting -> ConnectionStateDto(ConnectionStatusDto.CONNECTING)
            is ConnectionState.Connected -> ConnectionStateDto(ConnectionStatusDto.CONNECTED)
            is ConnectionState.Error -> ConnectionStateDto(ConnectionStatusDto.ERROR, state.message)
        }
}
