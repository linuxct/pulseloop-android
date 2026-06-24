package space.linuxct.pulseloop.ble

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object PulseEventBus {
    private val _events = MutableSharedFlow<PulseEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val events: SharedFlow<PulseEvent> = _events.asSharedFlow()

    fun publish(event: PulseEvent) {
        _events.tryEmit(event)
    }
}
