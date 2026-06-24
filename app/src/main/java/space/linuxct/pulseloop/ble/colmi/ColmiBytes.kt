package space.linuxct.pulseloop.ble.colmi

object ColmiBytes {
    fun u16(a: Byte, b: Byte): Int = (a.toInt() and 0xFF) or ((b.toInt() and 0xFF) shl 8)
    fun u24(a: Byte, b: Byte, c: Byte): Int =
        (a.toInt() and 0xFF) or ((b.toInt() and 0xFF) shl 8) or ((c.toInt() and 0xFF) shl 16)
    fun u32(a: Byte, b: Byte, c: Byte, d: Byte): Int =
        (a.toInt() and 0xFF) or ((b.toInt() and 0xFF) shl 8) or
        ((c.toInt() and 0xFF) shl 16) or ((d.toInt() and 0xFF) shl 24)
}
