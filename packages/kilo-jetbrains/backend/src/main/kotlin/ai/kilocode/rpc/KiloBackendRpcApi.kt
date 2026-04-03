@file:Suppress("UnstableApiUsage")

package ai.kilocode.rpc

import ai.kilocode.rpc.dto.ConnectionStateDto
import ai.kilocode.server.KiloConnectionService
import com.intellij.openapi.components.service
import kotlinx.coroutines.flow.Flow

class KiloBackendRpcApi : KiloRpcApi {
    override suspend fun connect() {
        service<KiloConnectionService>().connect()
    }

    override suspend fun state(): Flow<ConnectionStateDto> {
        return service<KiloConnectionService>().stream()
    }
}
