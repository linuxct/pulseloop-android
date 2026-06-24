package space.linuxct.pulseloop.ble.colmi

import java.util.UUID

object ColmiUUIDs {
    val SERVICE_V1: UUID = UUID.fromString("6e40fff0-b5a3-f393-e0a9-e50e24dcca9e")
    val SERVICE_V2: UUID = UUID.fromString("de5bf728-d711-4e47-af26-65e3012a5dc7")
    val WRITE: UUID      = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    val COMMAND: UUID    = UUID.fromString("de5bf72a-d711-4e47-af26-65e3012a5dc7")
    val NOTIFY_V1: UUID  = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    val NOTIFY_V2: UUID  = UUID.fromString("de5bf729-d711-4e47-af26-65e3012a5dc7")
}
