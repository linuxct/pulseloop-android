package space.linuxct.pulseloop.ble

import java.util.UUID

interface WearableDriver {
    val serviceUUIDs: List<UUID>
    val writeUUID: UUID
    val commandUUID: UUID?
    val notifyUUIDs: List<UUID>
    val batteryServiceUUID: UUID?
    val batteryCharUUID: UUID?

    fun frame(command: ByteArray): ByteArray

    fun usesCommandChannel(frame: ByteArray): Boolean = false

    fun ingest(data: ByteArray, fromCharacteristic: UUID): List<RingDecodedEvent>

    fun makeSyncEngine(writer: RingCommandWriter): RingSyncEngine
}
