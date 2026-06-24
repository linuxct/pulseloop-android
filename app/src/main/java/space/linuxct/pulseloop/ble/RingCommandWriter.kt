package space.linuxct.pulseloop.ble

interface RingCommandWriter {
    fun enqueue(command: ByteArray)
}
