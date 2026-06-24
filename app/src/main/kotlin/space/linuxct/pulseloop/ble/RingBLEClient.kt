package space.linuxct.pulseloop.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattConnectionSettings
import androidx.annotation.RequiresApi
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import space.linuxct.pulseloop.ble.colmi.ColmiCoordinator
import space.linuxct.pulseloop.ble.colmi.ColmiUUIDs
import space.linuxct.pulseloop.ble.jring.JringCoordinator
import space.linuxct.pulseloop.ble.jring.JringUUIDs
import space.linuxct.pulseloop.core.util.toHexString
import space.linuxct.pulseloop.domain.model.PacketDirection
import space.linuxct.pulseloop.domain.model.RingConnectionState
import space.linuxct.pulseloop.domain.model.RingDeviceType
import space.linuxct.pulseloop.domain.model.WearableCapability
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RingBLEClient"
private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

data class DiscoveredRing(
    val address: String,
    val name: String,
    val rssi: Int,
    val isLikelyRing: Boolean,
    val deviceType: RingDeviceType?
)

@Singleton
@SuppressLint("MissingPermission")
class RingBLEClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter
) : RingCommandWriter {

    private val coordinators: List<WearableCoordinator> = listOf(
        JringCoordinator(),
        ColmiCoordinator()
    )

    // --- Observable state ---
    private val _connectionState = MutableStateFlow(RingConnectionState.IDLE)
    val connectionState: StateFlow<RingConnectionState> = _connectionState.asStateFlow()

    private val _batteryPercent = MutableStateFlow<Int?>(null)
    val batteryPercent: StateFlow<Int?> = _batteryPercent.asStateFlow()

    private val _capabilities = MutableStateFlow<Set<WearableCapability>>(emptySet())
    val capabilities: StateFlow<Set<WearableCapability>> = _capabilities.asStateFlow()

    private val _discovered = MutableStateFlow<List<DiscoveredRing>>(emptyList())
    val discovered: StateFlow<List<DiscoveredRing>> = _discovered.asStateFlow()

    // --- GATT internals ---
    private var gatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null
    private var commandChar: BluetoothGattCharacteristic? = null
    private val notifyChars = mutableMapOf<UUID, BluetoothGattCharacteristic>()
    private var batteryChar: BluetoothGattCharacteristic? = null

    // Pending CCCD subscriptions: we must wait for each onDescriptorWrite before the next
    private val pendingNotifyChars = ArrayDeque<BluetoothGattCharacteristic>()
    private var allNotifySubscribed = false

    // --- Active driver / engine ---
    private var activeDriver: WearableDriver? = null
    private var activeSyncEngine: RingSyncEngine? = null
    private var activeCoordinator: WearableCoordinator? = null
    var activeDeviceType: RingDeviceType? = null
        private set

    // Tracks whether an HR or SpO2 measurement is currently in progress
    private val _hrActive  = AtomicBoolean(false)
    private val _spO2Active = AtomicBoolean(false)
    val hrActive:   Boolean get() = _hrActive.get()
    val spO2Active: Boolean get() = _spO2Active.get()

    // Sync engine forwarding — RingSyncCoordinator delegates to these
    fun measureHR()  { _hrActive.set(true);  activeSyncEngine?.startHeartRate() }
    fun stopHR()     { _hrActive.set(false); activeSyncEngine?.stopHeartRate() }
    fun measureSpO2() { _spO2Active.set(true);  activeSyncEngine?.startSpO2() }
    fun stopSpO2()   { _spO2Active.set(false); activeSyncEngine?.stopSpO2() }
    fun findDevice() = activeSyncEngine?.findDevice()
    fun setGoal(steps: Int) = activeSyncEngine?.setGoal(steps)

    // --- Write serialization ---
    private val writeQueue = ArrayDeque<Pair<ByteArray, Boolean>>() // (data, useCommandChannel)
    private val writeInFlight = AtomicBoolean(false)

    private var autoReconnect = true
    private var targetAddress: String? = null

    // --- Callback connected by RingCompanionService / RingSyncCoordinator ---
    var onConnected: (() -> Unit)? = null

    // -------------------------------------------------------------------------
    // Public API (called by RingCompanionService and PairingViewModel)
    // -------------------------------------------------------------------------

    fun connectToAddress(address: String, autoConnect: Boolean = false) {
        autoReconnect = true
        targetAddress = address
        val device = bluetoothAdapter.getRemoteDevice(address) ?: run {
            Log.w(TAG, "Device $address not found")
            return
        }
        beginConnect(device, autoConnect)
    }

    fun syncNow() {
        if (_connectionState.value != RingConnectionState.CONNECTED) return
        Log.d(TAG, "syncNow — triggering startup sequence")
        activeSyncEngine?.runStartup()
    }

    fun onCompanionDeviceDisappeared() {
        autoReconnect = false
        gatt?.disconnect()
    }

    fun startScan() {
        _discovered.value = emptyList()
        val scanner = bluetoothAdapter.bluetoothLeScanner ?: return
        // No service-UUID filters: jring advertises with name only ("SMART_RING"), not service UUIDs.
        // The scanCallback applies coordinator.matches() to identify ring type after receipt.
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()
        scanner.startScan(null, settings, scanCallback)
        _connectionState.value = RingConnectionState.SCANNING
    }

    fun stopScan() {
        bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        if (_connectionState.value == RingConnectionState.SCANNING) {
            _connectionState.value = RingConnectionState.IDLE
        }
    }

    fun disconnect() {
        autoReconnect = false
        gatt?.disconnect()
    }

    // -------------------------------------------------------------------------
    // RingCommandWriter
    // -------------------------------------------------------------------------

    override fun enqueue(command: ByteArray) {
        val driver = activeDriver ?: return
        val framed = driver.frame(command)
        val useCommand = driver.usesCommandChannel(framed)
        synchronized(writeQueue) { writeQueue.add(framed to useCommand) }
        Log.d(TAG, "enqueue cmd=0x%02X useCmd=$useCommand queueSize=${writeQueue.size}".format(framed.firstOrNull()?.toInt()?.and(0xFF) ?: 0))
        PulseEventBus.publish(PulseEvent.RawPacket(PacketDirection.OUTGOING, framed, space.linuxct.pulseloop.ble.RingDecodedEvent.CommandAck(framed.firstOrNull() ?: 0)))
        pumpWrites()
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private fun beginConnect(device: BluetoothDevice, autoConnect: Boolean = false) {
        _connectionState.value = RingConnectionState.CONNECTING
        resetState()
        val coordinatorForDevice = coordinators.firstOrNull {
            it.deviceType == RingDeviceType.JRING  // default jring; CDM won't need matching
        } ?: coordinators.first()
        installCoordinator(coordinatorForDevice)
        gatt = if (Build.VERSION.SDK_INT >= 37) {
            connectGattApi37(device, autoConnect)
        } else {
            @Suppress("DEPRECATION")
            device.connectGatt(context, autoConnect, gattCallback, BluetoothDevice.TRANSPORT_LE)
        }
    }

    @RequiresApi(37)
    private fun connectGattApi37(device: BluetoothDevice, autoConnect: Boolean): BluetoothGatt? =
        device.connectGatt(
            BluetoothGattConnectionSettings.Builder()
                .setTransport(BluetoothDevice.TRANSPORT_LE)
                .setAutoConnectEnabled(autoConnect)
                .build(),
            context.mainExecutor,
            gattCallback,
        )

    private fun installCoordinator(coordinator: WearableCoordinator) {
        activeCoordinator = coordinator
        activeDeviceType = coordinator.deviceType
        _capabilities.value = coordinator.capabilities
        val driver = coordinator.makeDriver(this)
        activeDriver = driver
        activeSyncEngine = driver.makeSyncEngine(this)
    }

    private fun resetState() {
        // Clear measurement flags unconditionally so a disconnect mid-measurement
        // never leaves them stuck true (the sync coroutine is cancelled, so stopHR/stopSpO2 won't run).
        _hrActive.set(false)
        _spO2Active.set(false)
        activeSyncEngine?.onDisconnected()
        writeChar = null
        commandChar = null
        notifyChars.clear()
        batteryChar = null
        pendingNotifyChars.clear()
        allNotifySubscribed = false
        writeInFlight.set(false)
        synchronized(writeQueue) { writeQueue.clear() }
    }

    private fun pumpWrites() {
        if (writeInFlight.get()) return
        val item = synchronized(writeQueue) { writeQueue.pollFirst() } ?: return
        val target = (if (item.second) (commandChar ?: writeChar) else writeChar) ?: run {
            Log.w(TAG, "pumpWrites: no target char (writeChar=$writeChar cmdChar=$commandChar), requeueing")
            synchronized(writeQueue) { writeQueue.addFirst(item) }
            return
        }
        if (writeInFlight.compareAndSet(false, true)) {
            val status = gatt?.writeCharacteristic(
                target, item.first, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            ) ?: BluetoothStatusCodes.ERROR_GATT_WRITE_NOT_ALLOWED
            Log.d(TAG, "writeCharacteristic cmd=0x%02X status=$status".format(item.first.firstOrNull()?.toInt()?.and(0xFF) ?: 0))
            if (status != BluetoothStatusCodes.SUCCESS) {
                Log.w(TAG, "writeCharacteristic FAILED status=$status, retrying in 300ms")
                // Keep writeInFlight=true to block all other pump attempts while we wait.
                synchronized(writeQueue) { writeQueue.addFirst(item) }
                Handler(Looper.getMainLooper()).postDelayed({
                    writeInFlight.set(false)
                    pumpWrites()
                }, 300)
            }
        }
    }

    private fun subscribeNextPendingNotify() {
        val gatt = gatt ?: return
        val char = pendingNotifyChars.pollFirst()
        if (char == null) {
            allNotifySubscribed = true
            // Give the GATT stack a moment to settle after CCCD writes before sending commands.
            Handler(Looper.getMainLooper()).postDelayed({ onFullyConnected() }, 600)
            return
        }
        gatt.setCharacteristicNotification(char, true)
        val cccd = char.getDescriptor(CCCD_UUID)
        if (cccd == null) {
            // No CCCD on this characteristic — skip it and move to the next.
            Log.d(TAG, "No CCCD on ${char.uuid}, skipping")
            subscribeNextPendingNotify()
            return
        }
        val result = gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        if (result != BluetoothStatusCodes.SUCCESS) {
            // GATT busy — put the char back and retry after a short delay.
            Log.w(TAG, "writeDescriptor for ${char.uuid} status=$result, retrying in 250ms")
            pendingNotifyChars.addFirst(char)
            Handler(Looper.getMainLooper()).postDelayed({ subscribeNextPendingNotify() }, 250)
        }
    }

    private fun onFullyConnected() {
        Log.d(TAG, "onFullyConnected — setting CONNECTED state")
        _connectionState.value = RingConnectionState.CONNECTED
        PulseEventBus.publish(PulseEvent.DeviceStateChanged(RingConnectionState.CONNECTED, targetAddress))
        activeDeviceType?.let { type ->
            PulseEventBus.publish(PulseEvent.DeviceIdentified(type, _capabilities.value))
        }
        onConnected?.invoke()
        // Battery read goes last so it doesn't race with the startup write queue.
        Handler(Looper.getMainLooper()).postDelayed({
            gatt?.readCharacteristic(batteryChar ?: return@postDelayed)
        }, 1500)
    }

    // -------------------------------------------------------------------------
    // GATT callback
    // -------------------------------------------------------------------------

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "STATE_CONNECTED — requesting MTU")
                    _connectionState.value = RingConnectionState.CONNECTING
                    gatt.requestMtu(512)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = if (autoReconnect) {
                        RingConnectionState.RECONNECTING
                    } else {
                        RingConnectionState.DISCONNECTED
                    }
                    PulseEventBus.publish(PulseEvent.DeviceStateChanged(_connectionState.value, null))
                    resetState()
                    if (autoReconnect) {
                        gatt.connect()
                    } else {
                        gatt.close()
                        this@RingBLEClient.gatt = null
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d(TAG, "MTU negotiated: $mtu (status=$status)")
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d(TAG, "onServicesDiscovered status=$status, ${gatt.services.size} services")
            for (svc in gatt.services) {
                Log.d(TAG, "  SVC ${svc.uuid}")
                for (ch in svc.characteristics) {
                    val descs = ch.descriptors.map { it.uuid }
                    Log.d(TAG, "    CHAR ${ch.uuid} props=${ch.properties} descs=$descs")
                }
            }
            val driver = activeDriver ?: return
            for (service in gatt.services) {
                when (service.uuid) {
                    in driver.serviceUUIDs -> {
                        for (char in service.characteristics) {
                            when (char.uuid) {
                                driver.writeUUID   -> writeChar = char
                                driver.commandUUID -> commandChar = char
                                in driver.notifyUUIDs -> {
                                    notifyChars[char.uuid] = char
                                    pendingNotifyChars.add(char)
                                }
                            }
                        }
                    }
                    driver.batteryServiceUUID -> {
                        batteryChar = service.characteristics.firstOrNull { it.uuid == driver.batteryCharUUID }
                    }
                }
            }
            subscribeNextPendingNotify()
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            subscribeNextPendingNotify()
        }

        // API 33 overload — value is a snapshot ByteArray, no copy needed
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val driver = activeDriver ?: return
            val events = driver.ingest(value, characteristic.uuid)
            for (decoded in events) {
                PulseEventBus.publish(PulseEvent.RawPacket(PacketDirection.INCOMING, value, decoded))
                for (pulse in RingEventBridge.map(decoded)) {
                    PulseEventBus.publish(pulse)
                }
                activeSyncEngine?.handle(decoded)
            }
        }

        // API 33 overload — value is a snapshot ByteArray
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (characteristic.uuid == activeDriver?.batteryCharUUID) {
                val pct = value.firstOrNull()?.toInt()?.and(0xFF) ?: return
                _batteryPercent.value = pct
                PulseEventBus.publish(PulseEvent.BatteryLevel(pct))
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Log.d(TAG, "onCharacteristicWrite uuid=${characteristic.uuid} status=$status")
            writeInFlight.set(false)
            pumpWrites()
        }
    }

    // -------------------------------------------------------------------------
    // BLE scan callback (for pairing screen discovery list)
    // -------------------------------------------------------------------------

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val name = result.device.name ?: return
            val address = result.device.address
            val info = AdvertisementInfo(
                serviceUUIDs = result.scanRecord?.serviceUuids?.map { it.uuid } ?: emptyList(),
                manufacturerData = result.scanRecord?.manufacturerSpecificData?.let {
                    if (it.size() > 0) it.valueAt(0) else null
                }
            )
            val matchedType = coordinators.firstOrNull { it.matches(name, info) }?.deviceType
            val ring = DiscoveredRing(
                address = address,
                name = name,
                rssi = result.rssi,
                isLikelyRing = matchedType != null,
                deviceType = matchedType
            )
            val current = _discovered.value.toMutableList()
            val idx = current.indexOfFirst { it.address == address }
            if (idx >= 0) current[idx] = ring else current.add(ring)
            current.sortWith(compareBy({ if (it.isLikelyRing) 0 else 1 }, { -it.rssi }))
            _discovered.value = current
        }
    }
}

private operator fun <K, V> Map<K, V>.contains(key: K?): Boolean = key != null && containsKey(key)
private operator fun List<UUID>.contains(uuid: UUID): Boolean = any { it == uuid }
private val List<UUID>.asSet: Set<UUID> get() = toSet()
