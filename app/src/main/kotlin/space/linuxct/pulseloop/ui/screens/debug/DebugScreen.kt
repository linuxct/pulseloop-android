package space.linuxct.pulseloop.ui.screens.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.data.db.entities.RawPacketRowEntity
import space.linuxct.pulseloop.data.db.entities.WearableLogEntity
import space.linuxct.pulseloop.ui.components.PulseCard
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import space.linuxct.pulseloop.ui.theme.LocalUiMode
import space.linuxct.pulseloop.ui.theme.UiMode
import space.linuxct.pulseloop.ui.viewmodel.DebugViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebugScreen(vm: DebugViewModel = hiltViewModel()) {
    val colors = LocalPulseColors.current
    val uiMode = LocalUiMode.current
    val state by vm.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize().then(
            if (uiMode == UiMode.MATERIAL_YOU) Modifier.statusBarsPadding() else Modifier
        )) {
            // Connection status bar
            PulseCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.debug_section_header), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, letterSpacing = 1.4.sp)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.debug_ble_state_label), fontSize = 13.sp, color = colors.textSecondary)
                        Text(state.connectionState.rawValue.replaceFirstChar { it.uppercase() }, fontSize = 13.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                    }
                    state.deviceName?.let { name ->
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.debug_device_label), fontSize = 13.sp, color = colors.textSecondary)
                            Text(name, fontSize = 13.sp, color = colors.textPrimary)
                        }
                    }
                    state.deviceAddress?.let { address ->
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.debug_address_label), fontSize = 13.sp, color = colors.textSecondary)
                            Text(address, fontSize = 13.sp, color = colors.textPrimary, fontFamily = FontFamily.Monospace)
                        }
                    }
                    if (state.batteryPercent != null) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.debug_battery_label), fontSize = 13.sp, color = colors.textSecondary)
                            Text("${state.batteryPercent}%", fontSize = 13.sp, color = colors.textPrimary)
                        }
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = colors.background,
                contentColor = colors.accent
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text(stringResource(R.string.debug_tab_packets, state.packets.size), modifier = Modifier.padding(vertical = 12.dp), fontSize = 13.sp)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text(stringResource(R.string.debug_tab_logs, state.logs.size), modifier = Modifier.padding(vertical = 12.dp), fontSize = 13.sp)
                }
            }

            if (selectedTab == 0) {
                PacketList(packets = state.packets)
            } else {
                LogList(logs = state.logs)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            SmallFloatingActionButton(
                onClick = { vm.exportFullDump { shareLauncher.launch(it) } },
                containerColor = colors.accent
            ) {
                Icon(Icons.Default.BugReport, contentDescription = stringResource(R.string.cd_debug_export), tint = Color.White)
            }
            FloatingActionButton(
                onClick = { vm.clearAll() },
                containerColor = colors.danger
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.cd_debug_clear), tint = Color.White)
            }
        }
    }
}

@Composable
private fun PacketList(packets: List<RawPacketRowEntity>) {
    val colors = LocalPulseColors.current
    val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (packets.isEmpty()) {
            item {
                Text(stringResource(R.string.debug_no_packets), fontSize = 13.sp, color = colors.textMuted, modifier = Modifier.padding(16.dp))
            }
        }
        items(packets.size) { i ->
            val p = packets[i]
            PulseCard(innerPadding = 10.dp, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(timeFmt.format(Date(p.timestamp)), fontSize = 11.sp, color = colors.textMuted, fontFamily = FontFamily.Monospace)
                        Text(p.directionRaw, fontSize = 11.sp, color = if (p.directionRaw == "in") colors.success else colors.accent, fontFamily = FontFamily.Monospace)
                        Text("0x%02X".format(p.commandId), fontSize = 11.sp, color = colors.textPrimary, fontFamily = FontFamily.Monospace)
                    }
                    Text(p.hexPayload, fontSize = 10.sp, color = colors.textMuted, fontFamily = FontFamily.Monospace, maxLines = 2)
                    Text(p.deviceTypeRaw, fontSize = 11.sp, color = colors.textSecondary)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun LogList(logs: List<WearableLogEntity>) {
    val colors = LocalPulseColors.current
    val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (logs.isEmpty()) {
            item {
                Text(stringResource(R.string.debug_no_logs), fontSize = 13.sp, color = colors.textMuted, modifier = Modifier.padding(16.dp))
            }
        }
        items(logs.size) { i ->
            val log = logs[i]
            PulseCard(innerPadding = 10.dp, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(timeFmt.format(Date(log.at)), fontSize = 11.sp, color = colors.textMuted, fontFamily = FontFamily.Monospace)
                        Text("[${log.level.uppercase()}]", fontSize = 11.sp, color = when (log.level) {
                            "error" -> colors.danger
                            "warn" -> colors.warning
                            else -> colors.textSecondary
                        })
                        Text(log.category, fontSize = 11.sp, color = colors.accent)
                    }
                    Text(log.message, fontSize = 12.sp, color = colors.textPrimary)
                    if (log.detail != null) {
                        Text(log.detail, fontSize = 10.sp, color = colors.textMuted, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
