package space.linuxct.pulseloop.ui.screens.export

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.data.db.entities.OtlpExportStateEntity
import space.linuxct.pulseloop.ui.theme.LocalUiMode
import space.linuxct.pulseloop.ui.theme.UiMode
import space.linuxct.pulseloop.ui.viewmodel.DataExportViewModel
import space.linuxct.pulseloop.ui.viewmodel.ExportState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataExportScreen(
    navController: NavController,
    vm: DataExportViewModel = hiltViewModel(),
) {
    val enabled by vm.enabled.collectAsState()
    val endpoint by vm.endpoint.collectAsState()
    val authType by vm.authType.collectAsState()
    val username by vm.username.collectAsState()
    val password by vm.password.collectAsState()
    val bearerToken by vm.bearerToken.collectAsState()
    val headerName by vm.headerName.collectAsState()
    val headerValue by vm.headerValue.collectAsState()
    val includeGps by vm.includeGps.collectAsState()
    val wifiOnly by vm.wifiOnly.collectAsState()
    val exportState by vm.exportState.collectAsState()
    val statuses by vm.statuses.collectAsState()
    // In V1 (LEGACY) the global AppHeaderBar already consumes the status bar, so the bar must NOT
    // add its own status-bar inset (that was the big gap). In V2 this screen is topmost, so it keeps
    // the default inset.
    val isMY = LocalUiMode.current == UiMode.MATERIAL_YOU

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_data_export)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                // Blend the bar into the background (matches the V1 Settings screen and avoids a
                // lighter surface band under the global header). Correct in V2 too (= scheme.background).
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                windowInsets = if (isMY) TopAppBarDefaults.windowInsets else WindowInsets(0),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ToggleRow(stringResource(R.string.otlp_enable_export), enabled, vm::setEnabled)

            Text(
                stringResource(R.string.otlp_intro),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = endpoint,
                onValueChange = vm::setEndpoint,
                label = { Text(stringResource(R.string.otlp_endpoint_label)) },
                placeholder = { Text(stringResource(R.string.otlp_endpoint_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.otlp_authentication), fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "NONE" to R.string.otlp_auth_none,
                    "BASIC" to R.string.otlp_auth_basic,
                    "BEARER" to R.string.otlp_auth_bearer,
                ).forEach { (type, labelRes) ->
                    FilterChip(
                        selected = authType == type,
                        onClick = { vm.setAuthType(type) },
                        label = { Text(stringResource(labelRes)) },
                    )
                }
            }

            when (authType) {
                "BASIC" -> {
                    OutlinedTextField(
                        value = username,
                        onValueChange = vm::setUsername,
                        label = { Text(stringResource(R.string.otlp_username)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = vm::setPassword,
                        label = { Text(stringResource(R.string.otlp_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                "BEARER" -> {
                    OutlinedTextField(
                        value = bearerToken,
                        onValueChange = vm::setBearerToken,
                        label = { Text(stringResource(R.string.otlp_bearer_token)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            HorizontalDivider()
            Text(stringResource(R.string.otlp_custom_header), fontWeight = FontWeight.Medium)
            Text(
                stringResource(R.string.otlp_custom_header_help),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = headerName,
                onValueChange = vm::setHeaderName,
                label = { Text(stringResource(R.string.otlp_header_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = headerValue,
                onValueChange = vm::setHeaderValue,
                label = { Text(stringResource(R.string.otlp_header_value)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()
            ToggleRow(stringResource(R.string.otlp_include_gps), includeGps, vm::setIncludeGps)
            Text(
                stringResource(R.string.otlp_include_gps_help),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ToggleRow(stringResource(R.string.otlp_wifi_only), wifiOnly, vm::setWifiOnly)

            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { vm.testConnection() },
                    enabled = exportState != ExportState.Testing,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.otlp_test_connection)) }
                Button(onClick = { vm.exportNow() }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.otlp_export_now))
                }
            }
            OutlinedButton(onClick = { vm.runBackfill() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.otlp_run_backfill))
            }

            when (val s = exportState) {
                ExportState.Testing -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                    Text(stringResource(R.string.otlp_testing))
                }
                is ExportState.Success -> Text(s.message, color = MaterialTheme.colorScheme.primary)
                is ExportState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
                ExportState.Idle -> {}
            }

            if (statuses.isNotEmpty()) {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.otlp_export_status), fontWeight = FontWeight.Medium)
                    Text(
                        stringResource(R.string.otlp_refresh),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { vm.refreshStatuses() }
                            .padding(start = 8.dp),
                        fontSize = 13.sp,
                    )
                }
                statuses.sortedBy { it.dataType }.forEach { StatusRow(it) }
            }

            Column(modifier = Modifier.padding(bottom = 24.dp)) {}
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private val statusFmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

@Composable
private fun StatusRow(state: OtlpExportStateEntity) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(state.dataType, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                if (state.lastTimestampMs > 0) "→ ${statusFmt.format(Date(state.lastTimestampMs))}" else "—",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.lastError?.let {
            Text(
                it,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
