package ai.kilocode.rpc

import ai.kilocode.rpc.dto.ConnectionStateDto
import com.intellij.platform.rpc.RemoteApiProviderService
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import kotlinx.coroutines.flow.Flow

@Rpc
interface KiloRpcApi : RemoteApi<Unit> {
    companion object {
        suspend fun getInstance(): KiloRpcApi {
            return RemoteApiProviderService.resolve(remoteApiDescriptor<KiloRpcApi>())
        }
    }

    suspend fun connect()

    suspend fun state(): Flow<ConnectionStateDto>
}
