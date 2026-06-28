package space.linuxct.pulseloop.ble.jring

object JringCommandId {
    const val TIME_SYNC: Byte                 = 0x01.toByte()
    const val ACTIVITY_QUERY_ACK: Byte        = 0x02.toByte()
    const val USER_INFO: Byte                 = 0x02.toByte() // 0x02 is CMD_SET_USER_INFO (age/sex/height/weight)
    const val CURRENT_ACTIVITY: Byte          = 0x03.toByte()
    const val FIND_RING: Byte                 = 0x04.toByte()
    const val AUTO_HR: Byte                   = 0x19.toByte()
    const val GOAL: Byte                      = 0x1a.toByte()
    const val PERCENT_STATUS: Byte            = 0x0b.toByte()
    const val STATUS: Byte                    = 0x0c.toByte()
    const val HISTORY_SUMMARY: Byte           = 0x10.toByte()
    const val SLEEP_TIMELINE: Byte            = 0x11.toByte()
    const val ACTIVITY_SUMMARY: Byte          = 0x13.toByte()
    const val HEART_RATE: Byte                = 0x14.toByte()
    const val HEART_RATE_STOP: Byte           = 0x15.toByte()
    const val HISTORY_MEASUREMENT: Byte       = 0x16.toByte()
    const val DEVICE_TIME: Byte               = 0x20.toByte()
    const val LOCALE: Byte                    = 0x21.toByte()
    // 0x23 starts a combined spot measurement (HR + BP + SpO2 + fatigue + stress + blood sugar);
    // the 0x24 response carries all of them. SpO2 is byte[4] of that response — i.e. the historic
    // "SpO2 measurement" was the combined measurement, only partially decoded.
    const val COMBINED_MEASUREMENT: Byte      = 0x23.toByte()
    const val COMBINED_RESULT: Byte           = 0x24.toByte()
    const val SPO2_START_STOP: Byte           = 0x23.toByte() // alias, same command
    const val SPO2_RESULT_PROGRESS: Byte      = 0x24.toByte() // alias, same response
    const val HEART_RATE_COMPLETE: Byte       = 0x27.toByte()
    const val SPO2_COMPLETE: Byte             = 0x28.toByte()
    const val BP_ADJUST: Byte                 = 0x33.toByte() // blood-pressure calibration (reference sys/dia)
    const val APP_IDENTIFIER: Byte            = 0x48.toByte()
    const val MODE: Byte                      = 0x52.toByte()
}
