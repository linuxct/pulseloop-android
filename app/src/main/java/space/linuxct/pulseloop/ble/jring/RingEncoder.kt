package space.linuxct.pulseloop.ble.jring

import java.util.TimeZone

class RingEncoder {

    fun makeStatusCommand(): ByteArray = hex("0c00000000000000000000000000000000000000")

    fun makeLocaleCommand(): ByteArray = hex("21656e2d55530000000000000000000000000000")

    fun makeActivityQueryCommand(): ByteArray = hex("0299b85a00000000000000000000000000000000")

    fun makeHistoryQueryCommand(): ByteArray = hex("1000000000000000000000000000000000000000")

    fun makeHistoryMeasurementQueryCommand(): ByteArray = hex("1600000000000000000000000000000000000000")

    fun makeHeartRateStartCommand(): ByteArray = hex("14b4000000000000000000000000000000000000")

    fun makeHeartRateStopCommand(): ByteArray = hex("1500000000000000000000000000000000000000")

    fun makeSpO2StartCommand(): ByteArray = hex("2301000000000000000000000000000000000000")

    fun makeSpO2StopCommand(): ByteArray = hex("2300000000000000000000000000000000000000")

    fun makeFindRingCommand(): ByteArray = hex("040a000000000000000000000000000000000000")

    fun makeGoalCommand(steps: Int): ByteArray {
        val bytes = ByteArray(20)
        bytes[0] = 0x1a
        val value = steps.toLong().coerceAtLeast(0)
        bytes[1] = (value and 0xff).toByte()
        bytes[2] = ((value shr 8) and 0xff).toByte()
        bytes[3] = ((value shr 16) and 0xff).toByte()
        bytes[4] = ((value shr 24) and 0xff).toByte()
        return bytes
    }

    fun makeAutomaticHeartRateCommand(enabled: Boolean, cadenceMinutes: Int = 30): ByteArray {
        val bytes = ByteArray(20)
        bytes[0] = 0x19
        bytes[1] = 0x00
        bytes[2] = 0x00
        bytes[3] = 0x17
        bytes[4] = 0x3b
        bytes[5] = if (enabled) 0x01 else 0x00
        bytes[6] = cadenceMinutes.coerceAtLeast(1).toByte()
        bytes[7] = 0x02
        return bytes
    }

    fun makeTimeSyncCommand(now: Long = System.currentTimeMillis()): ByteArray {
        val bytes = ByteArray(20)
        bytes[0] = 0x01
        val ts = (now / 1000L).toInt()
        bytes[1] = (ts and 0xff).toByte()
        bytes[2] = ((ts shr 8) and 0xff).toByte()
        bytes[3] = ((ts shr 16) and 0xff).toByte()
        bytes[4] = ((ts shr 24) and 0xff).toByte()
        val tzOffsetHours = TimeZone.getDefault().getOffset(now) / 3_600_000
        bytes[5] = tzOffsetHours.toByte()
        return bytes
    }

    private fun hex(s: String): ByteArray {
        check(s.length == 40) { "jring command must be 20 bytes (40 hex chars)" }
        return ByteArray(20) { i -> s.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }
}
