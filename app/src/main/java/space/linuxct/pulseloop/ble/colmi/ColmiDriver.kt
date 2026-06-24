package space.linuxct.pulseloop.ble.colmi

import space.linuxct.pulseloop.ble.RingCommandWriter
import space.linuxct.pulseloop.ble.RingDecodedEvent
import space.linuxct.pulseloop.ble.RingSyncEngine
import space.linuxct.pulseloop.ble.WearableDriver
import java.util.UUID

class ColmiDriver(writer: RingCommandWriter) : WearableDriver {

    private val decoder = ColmiDecoder()
    private val engine = ColmiSyncEngine(writer, decoder)

    // Big-data reassembly: keyed by type byte (0x25/0x27/0x2a)
    private val bigDataBuffers = mutableMapOf<Byte, ByteArray>()
    private var activeBigDataType: Byte? = null

    override val serviceUUIDs: List<UUID> = listOf(ColmiUUIDs.SERVICE_V1, ColmiUUIDs.SERVICE_V2)
    override val writeUUID: UUID = ColmiUUIDs.WRITE
    override val commandUUID: UUID = ColmiUUIDs.COMMAND
    override val notifyUUIDs: List<UUID> = listOf(ColmiUUIDs.NOTIFY_V1, ColmiUUIDs.NOTIFY_V2)
    override val batteryServiceUUID: UUID? = null
    override val batteryCharUUID: UUID? = null

    override fun frame(command: ByteArray): ByteArray {
        // Big-data requests (0xbc) are sent raw — not 16-byte framed
        return if (command.firstOrNull() == ColmiCommandId.BIG_DATA_V2) command
        else ColmiPacket.frame(command)
    }

    override fun usesCommandChannel(frame: ByteArray): Boolean =
        frame.firstOrNull() == ColmiCommandId.BIG_DATA_V2

    override fun ingest(data: ByteArray, fromCharacteristic: UUID): List<RingDecodedEvent> {
        return if (fromCharacteristic == ColmiUUIDs.NOTIFY_V2) ingestBigData(data)
        else ingestNormal(data)
    }

    override fun makeSyncEngine(writer: RingCommandWriter): RingSyncEngine = engine

    private fun ingestNormal(data: ByteArray): List<RingDecodedEvent> {
        val op = data.firstOrNull() ?: return emptyList()
        if (ColmiSyncEngine.isHistoryOpcode(op)) {
            return engine.handleHistoryFrame(data)
        }
        if (op == ColmiCommandId.REALTIME_HEART_RATE) {
            engine.observedRealtimeHeartRate()
        }
        return decoder.decodeNormal(data)
    }

    private fun ingestBigData(data: ByteArray): List<RingDecodedEvent> {
        val type: Byte
        if (data.firstOrNull() == ColmiCommandId.BIG_DATA_V2) {
            if (data.size < 4) return emptyList()
            type = data[1]
            bigDataBuffers[type] = data.copyOf()
        } else {
            val active = activeBigDataType ?: return listOf(
                RingDecodedEvent.Unknown(data.firstOrNull() ?: 0, data)
            )
            type = active
            val existing = bigDataBuffers[type] ?: return emptyList()
            bigDataBuffers[type] = existing + data
        }

        val buffer = bigDataBuffers[type] ?: return emptyList()
        if (buffer.size < 4) return emptyList()
        val expectedLength = ColmiBytes.u16(buffer[2], buffer[3])
        if (buffer.size < expectedLength + 6) {
            activeBigDataType = type
            return emptyList()
        }

        bigDataBuffers.remove(type)
        activeBigDataType = bigDataBuffers.keys.firstOrNull()

        val events = decoder.decodeBigData(buffer)
        engine.handleBigDataComplete(type)
        return events
    }
}
