package space.linuxct.pulseloop.ble

object RingEventBridge {

    fun map(decoded: RingDecodedEvent): List<PulseEvent> = when (decoded) {
        is RingDecodedEvent.ActivityUpdate -> listOf(
            PulseEvent.ActivityUpdate(decoded.timestamp, decoded.steps, decoded.distanceMeters, decoded.calories)
        )
        is RingDecodedEvent.ActivityBucket -> {
            if (decoded.steps > 5000 || decoded.distanceMeters > 6000) emptyList()
            else listOf(PulseEvent.ActivityBucket(decoded.timestamp, decoded.steps, decoded.distanceMeters))
        }
        is RingDecodedEvent.HeartRateSample -> {
            if (decoded.bpm !in 30..220) emptyList()
            else listOf(PulseEvent.HeartRateSample(decoded.bpm, decoded.timestamp))
        }
        is RingDecodedEvent.HeartRateComplete ->
            listOf(PulseEvent.HeartRateComplete(decoded.timestamp))
        is RingDecodedEvent.Spo2Progress ->
            listOf(PulseEvent.Spo2Progress(decoded.percent, decoded.timestamp))
        is RingDecodedEvent.Spo2Result ->
            listOf(PulseEvent.Spo2Result(decoded.value, decoded.timestamp))
        is RingDecodedEvent.Spo2Complete ->
            listOf(PulseEvent.Spo2Complete(decoded.timestamp))
        is RingDecodedEvent.SleepTimeline ->
            listOf(PulseEvent.SleepTimeline(decoded.timestamp, decoded.stages))
        is RingDecodedEvent.HistoryMeasurement ->
            listOf(PulseEvent.HistoryMeasurement(decoded.kind, decoded.value, decoded.timestamp))
        is RingDecodedEvent.StressSample -> {
            if (decoded.value !in 1..100) emptyList()
            else listOf(PulseEvent.StressSample(decoded.value, decoded.timestamp))
        }
        is RingDecodedEvent.HrvSample -> {
            if (decoded.value !in 1..300) emptyList()
            else listOf(PulseEvent.HrvSample(decoded.value, decoded.timestamp))
        }
        is RingDecodedEvent.TemperatureSample -> {
            if (decoded.celsius !in 30.0..45.0) emptyList()
            else listOf(PulseEvent.TemperatureSample(decoded.celsius, decoded.timestamp))
        }
        is RingDecodedEvent.HistorySyncProgress ->
            listOf(PulseEvent.SyncProgress(decoded.stage))
        is RingDecodedEvent.HistorySyncFinished ->
            listOf(PulseEvent.SyncProgress("done"))
        is RingDecodedEvent.Battery ->
            listOf(PulseEvent.BatteryLevel(decoded.percent))
        is RingDecodedEvent.Status, is RingDecodedEvent.TimeSyncAck,
        is RingDecodedEvent.CommandAck, is RingDecodedEvent.Unknown -> emptyList()
    }
}
