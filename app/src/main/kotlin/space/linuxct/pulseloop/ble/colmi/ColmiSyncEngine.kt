package space.linuxct.pulseloop.ble.colmi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.linuxct.pulseloop.ble.PulseEvent
import space.linuxct.pulseloop.ble.PulseEventBus
import space.linuxct.pulseloop.ble.RingCommandWriter
import space.linuxct.pulseloop.ble.RingDecodedEvent
import space.linuxct.pulseloop.ble.RingSyncEngine
import java.util.Calendar

class ColmiSyncEngine(
    private val writer: RingCommandWriter,
    private val decoder: ColmiDecoder
) : RingSyncEngine {

    private val encoder = ColmiEncoder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private enum class Stage {
        IDLE, ACTIVITY, HEART_RATE, STRESS, SPO2, SLEEP, HRV, TEMPERATURE, DONE
    }

    private var stage = Stage.IDLE
    private var daysAgo = 0
    private var syncDayMs = todayMidnightMs()

    companion object {
        fun isHistoryOpcode(op: Byte): Boolean =
            op == ColmiCommandId.SYNC_ACTIVITY ||
            op == ColmiCommandId.SYNC_HEART_RATE ||
            op == ColmiCommandId.SYNC_STRESS ||
            op == ColmiCommandId.SYNC_HRV
    }

    override fun runStartup() {
        writer.enqueue(encoder.phoneName())
        writer.enqueue(encoder.setDateTime())
        writer.enqueue(encoder.userPreferences())
        writer.enqueue(encoder.battery())
        writer.enqueue(encoder.readPref(ColmiCommandId.AUTO_HR_PREF))
        writer.enqueue(encoder.readPref(ColmiCommandId.AUTO_STRESS_PREF))
        writer.enqueue(encoder.readPref(ColmiCommandId.AUTO_SPO2_PREF))
        writer.enqueue(encoder.readPref(ColmiCommandId.AUTO_HRV_PREF))
        writer.enqueue(encoder.readTempPref())
        writer.enqueue(encoder.readGoals())
        writer.enqueue(encoder.writePref(ColmiCommandId.AUTO_SPO2_PREF, true))
        writer.enqueue(encoder.writePref(ColmiCommandId.AUTO_STRESS_PREF, true))
        writer.enqueue(encoder.writePref(ColmiCommandId.AUTO_HRV_PREF, true))
        startHistorySync()
    }

    override fun handle(event: RingDecodedEvent) { /* Colmi uses driver hooks — no-op here */ }

    // --- Driver hooks ---

    fun handleHistoryFrame(data: ByteArray): List<RingDecodedEvent> {
        val events = decoder.decodeHistory(data, syncDayMs)
        advanceAfterPagedFrame(data)
        armWatchdog()
        return events
    }

    fun handleBigDataComplete(type: Byte) {
        when (type) {
            ColmiCommandId.BIG_DATA_SPO2  -> { stage = Stage.SLEEP; requestSleep(); armWatchdog() }
            ColmiCommandId.BIG_DATA_SLEEP -> { stage = Stage.HRV; daysAgo = 0; requestHrv(); armWatchdog() }
            ColmiCommandId.BIG_DATA_TEMPERATURE -> finishSync()
        }
    }

    fun observedRealtimeHeartRate() {
        if (!realtimeHrActive) return
        realtimeHrPacketCount = (realtimeHrPacketCount + 1) % 30
        if (realtimeHrPacketCount == 0) writer.enqueue(encoder.realtimeHeartRateContinue())
    }

    // --- Measurement actions ---

    override fun startHeartRate() {
        realtimeHrActive = true
        realtimeHrPacketCount = 0
        writer.enqueue(encoder.realtimeHeartRate(true))
    }

    override fun stopHeartRate() {
        if (manualHrActive) {
            manualHrActive = false
            writer.enqueue(encoder.manualHeartRate(false))
        }
        if (!realtimeHrActive) return
        realtimeHrActive = false
        writer.enqueue(encoder.realtimeHeartRate(false))
    }

    override fun startSpO2() {
        writer.enqueue(encoder.bigDataSpo2())
    }

    override fun stopSpO2() { /* Colmi SpO2 is all-day background — no explicit stop */ }

    override fun findDevice() {
        writer.enqueue(encoder.findDevice())
    }

    override fun setGoal(steps: Int) { /* Unverified payload — skip silent write */ }

    fun measureHeartRateSpot() {
        manualHrActive = true
        writer.enqueue(encoder.manualHeartRate(true))
    }

    // --- Internals ---

    private var realtimeHrActive = false
    private var realtimeHrPacketCount = 0
    private var manualHrActive = false

    private var watchdogJob: Job? = null
    private val activityWatchdogMs = 20_000L
    private val defaultWatchdogMs  = 10_000L

    private fun startHistorySync() {
        daysAgo = 0
        stage = Stage.ACTIVITY
        PulseEventBus.publish(PulseEvent.ActivitySyncReset(7))
        requestActivity()
        armWatchdog()
    }

    private fun armWatchdog() {
        watchdogJob?.cancel()
        val expected = stage
        val timeout = if (expected == Stage.ACTIVITY) activityWatchdogMs else defaultWatchdogMs
        watchdogJob = scope.launch {
            delay(timeout)
            if (stage == expected) forceAdvanceStage(expected)
        }
    }

    private fun forceAdvanceStage(stuck: Stage) {
        when (stuck) {
            Stage.ACTIVITY     -> { daysAgo = 0; stage = Stage.HEART_RATE; requestHeartRate() }
            Stage.HEART_RATE   -> { stage = Stage.STRESS; requestStress() }
            Stage.STRESS       -> { stage = Stage.SPO2; requestSpo2() }
            Stage.SPO2         -> { stage = Stage.SLEEP; requestSleep() }
            Stage.SLEEP        -> { daysAgo = 0; stage = Stage.HRV; requestHrv() }
            Stage.HRV          -> { stage = Stage.TEMPERATURE; requestTemperature() }
            Stage.TEMPERATURE  -> finishSync()
            else               -> return
        }
        if (stage != Stage.DONE) armWatchdog()
    }

    private fun requestActivity()    { syncDayMs = dayMidnightMs(daysAgo); writer.enqueue(encoder.syncActivity(daysAgo)) }
    private fun requestHeartRate()   { syncDayMs = dayMidnightMs(daysAgo); writer.enqueue(encoder.syncHeartRate(syncDayMs / 1000)) }
    private fun requestStress()      { syncDayMs = todayMidnightMs(); writer.enqueue(encoder.syncStress()) }
    private fun requestHrv()         { syncDayMs = dayMidnightMs(daysAgo); writer.enqueue(encoder.syncHrv(daysAgo)) }
    private fun requestSpo2()        { writer.enqueue(encoder.bigDataSpo2()) }
    private fun requestSleep()       { writer.enqueue(encoder.bigDataSleep()) }
    private fun requestTemperature() { writer.enqueue(encoder.bigDataTemperature()) }

    private fun advanceAfterPagedFrame(data: ByteArray) {
        val packetNr = ColmiDecoder.historyPacketNumber(data) ?: return
        val isEmpty  = packetNr == 0xFF
        val dayDone  = isEmpty || isTerminalPacket(data)
        if (!dayDone) return
        when (stage) {
            Stage.ACTIVITY   -> if (daysAgo < 7) { daysAgo++; requestActivity() } else { daysAgo = 0; stage = Stage.HEART_RATE; requestHeartRate() }
            Stage.HEART_RATE -> if (daysAgo < 7) { daysAgo++; requestHeartRate() } else { stage = Stage.STRESS; requestStress() }
            Stage.STRESS     -> { stage = Stage.SPO2; requestSpo2() }
            Stage.HRV        -> if (daysAgo < 6) { daysAgo++; requestHrv() } else { stage = Stage.TEMPERATURE; requestTemperature() }
            else             -> Unit
        }
    }

    private fun isTerminalPacket(data: ByteArray): Boolean {
        if (data.size < 7) return false
        return when (data[0]) {
            ColmiCommandId.SYNC_STRESS, ColmiCommandId.SYNC_HRV -> (data[1].toInt() and 0xFF) == 4
            ColmiCommandId.SYNC_ACTIVITY -> (data[5].toInt() and 0xFF) == (data[6].toInt() and 0xFF) - 1
            else -> false
        }
    }

    private fun finishSync() {
        stage = Stage.DONE
        watchdogJob?.cancel()
        watchdogJob = null
        PulseEventBus.publish(PulseEvent.SyncProgress("done"))
    }

    private fun todayMidnightMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun dayMidnightMs(daysAgo: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
