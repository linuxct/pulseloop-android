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

    // 0x23 starts a combined spot measurement (HR + BP + SpO2 + fatigue + stress + blood sugar);
    // the 0x24 response carries all of them. These are the same bytes historically used for SpO2.
    fun makeCombinedMeasurementStartCommand(): ByteArray = hex("2301000000000000000000000000000000000000")
    fun makeCombinedMeasurementStopCommand(): ByteArray = hex("2300000000000000000000000000000000000000")

    // Kept as aliases so existing SpO2-trigger call sites keep working.
    fun makeSpO2StartCommand(): ByteArray = makeCombinedMeasurementStartCommand()
    fun makeSpO2StopCommand(): ByteArray = makeCombinedMeasurementStopCommand()

    /**
     * User info / personal data (0x02 CMD_SET_USER_INFO). Feeds the ring's blood-sugar
     * (profile-derived estimate) and calorie algorithms. Blood pressure is a direct PPG sensor
     * reading and does NOT use user info. Mirrors the official SXR SDK's setUserInfo:
     *   byte[0] = 0x02
     *   byte[1] = age (low 7 bits) | 0x80 if male
     *   byte[2] = height (cm)
     *   byte[3] = weight (kg)
     *   byte[4] = unit flag (0 = metric)
     * Always transmitted in metric so the ring interprets values unambiguously.
     */
    fun makeUserInfoCommand(ageYears: Int, isMale: Boolean, heightCm: Int, weightKg: Int): ByteArray {
        val bytes = ByteArray(20)
        bytes[0] = JringCommandId.USER_INFO
        val age = ageYears.coerceIn(0, 127)
        bytes[1] = (age or if (isMale) 0x80 else 0x00).toByte()
        bytes[2] = heightCm.coerceIn(0, 255).toByte()
        bytes[3] = weightKg.coerceIn(0, 255).toByte()
        bytes[4] = 0x00 // metric
        return bytes
    }

    /**
     * Blood-pressure calibration (0x33). Sends a reference systolic/diastolic (e.g. from a cuff)
     * so the ring offsets its readings to match. Mirrors the official SDK's setBPAdjust: each value
     * is a little-endian u16.
     *   byte[0] = 0x33  byte[1..2] = systolic (LE u16)  byte[3..4] = diastolic (LE u16)
     */
    fun makeBPAdjustCommand(systolic: Int, diastolic: Int): ByteArray {
        val bytes = ByteArray(20)
        bytes[0] = JringCommandId.BP_ADJUST
        bytes[1] = (systolic and 0xff).toByte()
        bytes[2] = ((systolic shr 8) and 0xff).toByte()
        bytes[3] = (diastolic and 0xff).toByte()
        bytes[4] = ((diastolic shr 8) and 0xff).toByte()
        return bytes
    }

    fun makeFindRingCommand(): ByteArray     = hex("040a000000000000000000000000000000000000")
    fun makeStopFindRingCommand(): ByteArray = hex("0400000000000000000000000000000000000000")

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
