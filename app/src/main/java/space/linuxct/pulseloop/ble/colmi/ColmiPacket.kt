package space.linuxct.pulseloop.ble.colmi

data class ColmiPacket(val bytes: ByteArray) {

    companion object {
        fun frame(content: ByteArray): ByteArray {
            val buffer = ByteArray(16)
            val count = minOf(content.size, 15)
            content.copyInto(buffer, 0, 0, count)
            var checksum = 0
            for (i in 0..14) checksum = (checksum + (buffer[i].toInt() and 0xFF)) and 0xFF
            buffer[15] = checksum.toByte()
            return buffer
        }

        fun validate(data: ByteArray): ColmiPacket? {
            if (data.size != 16) return null
            var checksum = 0
            for (i in 0..14) checksum = (checksum + (data[i].toInt() and 0xFF)) and 0xFF
            if (checksum.toByte() != data[15]) return null
            return ColmiPacket(data)
        }
    }

    override fun equals(other: Any?): Boolean = other is ColmiPacket && bytes.contentEquals(other.bytes)
    override fun hashCode(): Int = bytes.contentHashCode()
}
