package space.linuxct.pulseloop.ui.screens.pairing

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import space.linuxct.pulseloop.ble.DiscoveredRing
import space.linuxct.pulseloop.ble.RingBLEClient
import space.linuxct.pulseloop.data.db.entities.DeviceEntity
import space.linuxct.pulseloop.domain.model.RingConnectionState
import space.linuxct.pulseloop.domain.repository.DeviceRepository
import space.linuxct.pulseloop.ui.components.PrimaryButton
import space.linuxct.pulseloop.ui.components.PulseCard
import space.linuxct.pulseloop.ui.components.SecondaryButton
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import javax.inject.Inject

private val BLE_PERMISSIONS = arrayOf(
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.BLUETOOTH_CONNECT
)

@HiltViewModel
class PairingViewModel @Inject constructor(
    val bleClient: RingBLEClient,
    private val deviceRepo: DeviceRepository
) : androidx.lifecycle.ViewModel() {
    val connectionState: StateFlow<RingConnectionState> = bleClient.connectionState
    val discovered: StateFlow<List<DiscoveredRing>> = bleClient.discovered

    fun startScan() = bleClient.startScan()
    fun stopScan() = bleClient.stopScan()

    fun connect(ring: DiscoveredRing) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            deviceRepo.upsert(DeviceEntity(
                id = ring.address,
                name = ring.name,
                macAddress = ring.address,
                stateRaw = RingConnectionState.CONNECTING.rawValue,
                deviceTypeRaw = ring.deviceType?.rawValue ?: "",
                capabilitiesRaw = "",
                batteryLevel = null, firmwareVersion = null, hardwareVersion = null,
                associationId = null, lastSeenAt = null, pairedAt = now
            ))
        }
        bleClient.connectToAddress(ring.address)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(onDone: () -> Unit, vm: PairingViewModel = hiltViewModel()) {
    val colors = LocalPulseColors.current
    val context = LocalContext.current
    val connectionState by vm.connectionState.collectAsState()
    val discovered by vm.discovered.collectAsState()
    val isScanning = connectionState == RingConnectionState.SCANNING
    val isConnected = connectionState == RingConnectionState.CONNECTED

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) vm.startScan()
    }

    fun requestScan() {
        val allGranted = BLE_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) vm.startScan() else permissionLauncher.launch(BLE_PERMISSIONS)
    }

    DisposableEffect(Unit) {
        onDispose { vm.stopScan() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Add your ring", color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(
                        "Swipe to find your model, then tap to connect. You can also explore first and pair later.",
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                }

                if (isConnected) {
                    item {
                        PulseCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = colors.success,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Connected!", fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = colors.textPrimary)
                                Text("Your ring is paired and ready.", fontSize = 14.sp, color = colors.textMuted)
                            }
                        }
                        PrimaryButton(title = "Continue", onClick = onDone)
                    }
                } else {
                    item {
                        // Ring icon card
                        PulseCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(colors.accentSoft, CircleShape)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.BluetoothSearching,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                Text("Smart Ring", fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = colors.textPrimary)
                                Text("Make sure your ring is nearby and charged.", fontSize = 13.sp, color = colors.textMuted)
                            }
                        }
                    }

                    if (!isScanning) {
                        item {
                            PrimaryButton(
                                title = "Scan for rings",
                                onClick = { requestScan() }
                            )
                        }
                    } else {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.accent
                                )
                                Text(
                                    when (connectionState) {
                                        RingConnectionState.CONNECTING, RingConnectionState.RECONNECTING -> "Connecting…"
                                        else -> "Looking for rings nearby…"
                                    },
                                    fontSize = 13.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        if (discovered.isEmpty()) {
                            item {
                                PulseCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                                    ) {
                                        Text("No rings found yet", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = colors.textPrimary)
                                        Text("Wake the ring by tapping or moving it, and keep it close.", fontSize = 12.sp, color = colors.textMuted)
                                    }
                                }
                            }
                        } else {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    discovered.forEach { ring ->
                                        RingRow(ring = ring, onClick = { vm.connect(ring) })
                                    }
                                }
                            }
                        }

                        item {
                            SecondaryButton(title = "Stop scanning", onClick = { vm.stopScan() })
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun RingRow(ring: DiscoveredRing, onClick: () -> Unit) {
    val colors = LocalPulseColors.current
    PulseCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                if (ring.isLikelyRing) Icons.Default.RadioButtonChecked else Icons.Default.Bluetooth,
                contentDescription = null,
                tint = if (ring.isLikelyRing) colors.accent else colors.textMuted,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(ring.name, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = colors.textPrimary)
                ring.deviceType?.let { type ->
                    Text(type.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp, color = colors.accent)
                }
            }
            Text(
                "${ring.rssi} dBm",
                fontSize = 12.sp,
                color = colors.textMuted
            )
        }
    }
}
