package space.linuxct.pulseloop.core.util

import java.util.UUID

fun ByteArray.getUInt8(offset: Int): Int = this[offset].toInt() and 0xFF

fun ByteArray.getUInt16Le(offset: Int): Int =
    (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)

fun ByteArray.getUInt24Le(offset: Int): Int =
    (this[offset].toInt() and 0xFF) or
    ((this[offset + 1].toInt() and 0xFF) shl 8) or
    ((this[offset + 2].toInt() and 0xFF) shl 16)

fun ByteArray.getUInt32Le(offset: Int): Long =
    (this[offset].toLong() and 0xFF) or
    ((this[offset + 1].toLong() and 0xFF) shl 8) or
    ((this[offset + 2].toLong() and 0xFF) shl 16) or
    ((this[offset + 3].toLong() and 0xFF) shl 24)

fun ByteArray.getIntLe(offset: Int): Int = getUInt16Le(offset)

fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

fun Int.toByteArrayLe(size: Int = 4): ByteArray {
    val bytes = ByteArray(size)
    for (i in 0 until size) bytes[i] = ((this shr (i * 8)) and 0xFF).toByte()
    return bytes
}

fun Long.toByteArrayLe(size: Int = 4): ByteArray {
    val bytes = ByteArray(size)
    for (i in 0 until size) bytes[i] = ((this shr (i * 8)) and 0xFF).toByte()
    return bytes
}

fun ByteArray.bcdToDec(offset: Int): Int {
    val b = this[offset].toInt() and 0xFF
    return (b shr 4) * 10 + (b and 0x0F)
}

fun newUUID(): String = UUID.randomUUID().toString()

fun ByteArray.copyOfSafe(fromIndex: Int, toIndex: Int): ByteArray {
    val end = minOf(toIndex, this.size)
    return if (fromIndex >= end) ByteArray(0) else copyOfRange(fromIndex, end)
}
