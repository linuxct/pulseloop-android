package space.linuxct.pulseloop.ble.jring

import space.linuxct.pulseloop.ble.RingCommandWriter
import space.linuxct.pulseloop.ble.RingDecodedEvent
import space.linuxct.pulseloop.ble.RingSyncEngine
import space.linuxct.pulseloop.ble.WearableDriver
import java.util.UUID

class JringDriver : WearableDriver {

    private val decoder = RingDecoder()

    override val serviceUUIDs: List<UUID> = listOf(JringUUIDs.SERVICE)
    override val writeUUID: UUID           = JringUUIDs.WRITE
    override val commandUUID: UUID?        = null
    override val notifyUUIDs: List<UUID>   = listOf(JringUUIDs.NOTIFY)
    override val batteryServiceUUID: UUID? = JringUUIDs.BATTERY_SVC
    override val batteryCharUUID: UUID?    = JringUUIDs.BATTERY

    override fun frame(command: ByteArray): ByteArray = command

    override fun ingest(data: ByteArray, fromCharacteristic: UUID): List<RingDecodedEvent> =
        listOf(decoder.decode(data))

    override fun makeSyncEngine(writer: RingCommandWriter): RingSyncEngine =
        JringSyncEngine(writer)
}
