package ai.kilocode.jetbrains

/** Represents the state of the Kilo CLI server connection. */
sealed class KiloConnection {
    data object Ready : KiloConnection()
    data class Error(val message: String, val details: String? = null) : KiloConnection()
}
