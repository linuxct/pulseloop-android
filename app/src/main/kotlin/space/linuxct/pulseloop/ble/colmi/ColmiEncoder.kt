package space.linuxct.pulseloop.ble.colmi

import java.util.Calendar

class ColmiEncoder {

    fun phoneName(): ByteArray = byteArrayOf(ColmiCommandId.PHONE_NAME, 0x02, 0x0a, 'P'.code.toByte(), 'L'.code.toByte())

    fun setDateTime(nowMs: Long = System.currentTimeMillis()): ByteArray {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        fun bcd(v: Int): Byte = ((v % 100 / 10) shl 4 or (v % 10)).toByte()
        return byteArrayOf(
            ColmiCommandId.SET_DATE_TIME,
            bcd(cal.get(Calendar.YEAR) % 2000),
            bcd(cal.get(Calendar.MONTH) + 1),
            bcd(cal.get(Calendar.DAY_OF_MONTH)),
            bcd(cal.get(Calendar.HOUR_OF_DAY)),
            bcd(cal.get(Calendar.MINUTE)),
            bcd(cal.get(Calendar.SECOND))
        )
    }

    fun userPreferences(metric: Boolean = true, gender: Byte = 0x02, age: Byte = 25, heightCm: Byte = 175.toByte(), weightKg: Byte = 70): ByteArray =
        byteArrayOf(
            ColmiCommandId.PREFERENCES, ColmiCommandId.PREF_WRITE,
            0x00, if (metric) 0x00 else 0x01, gender, age, heightCm, weightKg,
            0x00, 0x00, 0x00
        )

    fun battery(): ByteArray = byteArrayOf(ColmiCommandId.BATTERY)

    fun readPref(command: Byte): ByteArray = byteArrayOf(command, ColmiCommandId.PREF_READ)

    fun writePref(command: Byte, enabled: Boolean): ByteArray =
        byteArrayOf(command, ColmiCommandId.PREF_WRITE, if (enabled) 0x01 else 0x00)

    fun readTempPref(): ByteArray = byteArrayOf(ColmiCommandId.AUTO_TEMP_PREF, 0x03, ColmiCommandId.PREF_READ)

    fun readGoals(): ByteArray = byteArrayOf(ColmiCommandId.GOALS, ColmiCommandId.PREF_READ)

    fun manualHeartRate(enable: Boolean = true): ByteArray =
        byteArrayOf(ColmiCommandId.MANUAL_HEART_RATE, if (enable) 0x01 else 0x02)

    fun realtimeHeartRate(enable: Boolean): ByteArray =
        byteArrayOf(ColmiCommandId.REALTIME_HEART_RATE, if (enable) 0x01 else 0x02)

    fun realtimeHeartRateContinue(): ByteArray =
        byteArrayOf(ColmiCommandId.REALTIME_HEART_RATE, 0x03)

    fun findDevice(): ByteArray = byteArrayOf(ColmiCommandId.FIND_DEVICE, 0x55.toByte(), 0xAA.toByte())

    fun powerOff(): ByteArray = byteArrayOf(ColmiCommandId.POWER_OFF, 0x01)

    fun factoryReset(): ByteArray = byteArrayOf(ColmiCommandId.FACTORY_RESET, 0x66, 0x66)

    fun syncActivity(daysAgo: Int): ByteArray =
        byteArrayOf(ColmiCommandId.SYNC_ACTIVITY, daysAgo.coerceIn(0, 255).toByte(), 0x0f, 0x00, 0x5f, 0x01)

    fun syncHeartRate(fromUnixSeconds: Long): ByteArray {
        val ts = fromUnixSeconds.toUInt()
        return byteArrayOf(
            ColmiCommandId.SYNC_HEART_RATE,
            (ts and 0xFFu).toByte(),
            ((ts shr 8) and 0xFFu).toByte(),
            ((ts shr 16) and 0xFFu).toByte(),
            ((ts shr 24) and 0xFFu).toByte()
        )
    }

    fun syncStress(): ByteArray = byteArrayOf(ColmiCommandId.SYNC_STRESS)

    fun syncHrv(daysAgo: Int): ByteArray {
        val d = daysAgo.toLong().toUInt()
        return byteArrayOf(
            ColmiCommandId.SYNC_HRV,
            (d and 0xFFu).toByte(),
            ((d shr 8) and 0xFFu).toByte(),
            ((d shr 16) and 0xFFu).toByte(),
            ((d shr 24) and 0xFFu).toByte()
        )
    }

    fun bigDataSpo2(): ByteArray =
        byteArrayOf(ColmiCommandId.BIG_DATA_V2, ColmiCommandId.BIG_DATA_SPO2, 0x01, 0x00, 0xff.toByte(), 0x00, 0xff.toByte())

    fun bigDataSleep(): ByteArray =
        byteArrayOf(ColmiCommandId.BIG_DATA_V2, ColmiCommandId.BIG_DATA_SLEEP, 0x01, 0x00, 0xff.toByte(), 0x00, 0xff.toByte())

    fun bigDataTemperature(): ByteArray =
        byteArrayOf(ColmiCommandId.BIG_DATA_V2, ColmiCommandId.BIG_DATA_TEMPERATURE, 0x01, 0x00, 0x3e, 0x81.toByte(), 0x02)
}
