package space.linuxct.pulseloop.ui.screens.record

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import space.linuxct.pulseloop.ble.PulseEvent
import space.linuxct.pulseloop.ble.PulseEventBus
import space.linuxct.pulseloop.data.db.entities.ActivityGpsPointEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySessionEntity
import space.linuxct.pulseloop.domain.model.ActivitySessionStatus
import space.linuxct.pulseloop.domain.repository.ActivityRepository
import space.linuxct.pulseloop.ui.charts.HrLineChart
import space.linuxct.pulseloop.ui.components.PrimaryButton
import space.linuxct.pulseloop.ui.components.PulseCard
import space.linuxct.pulseloop.ui.components.RouteMapCard
import space.linuxct.pulseloop.ui.components.RouteMapCardLatLng
import space.linuxct.pulseloop.ui.components.SecondaryButton
import space.linuxct.pulseloop.ui.navigation.NavRoute
import space.linuxct.pulseloop.ui.screens.activity.activityLabel
import space.linuxct.pulseloop.ui.screens.activity.formatDuration
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import space.linuxct.pulseloop.workout.LiveWorkoutManager
import space.linuxct.pulseloop.workout.WorkoutLiveState
import javax.inject.Inject

// ─── Activity types ───────────────────────────────────────────────────────────

private data class ActivityKind(
    val type: String,
    val label: String,
    val icon: ImageVector,
    val helper: String,
    val gpsCapable: Boolean = true
)

private val ACTIVITY_KINDS = listOf(
    ActivityKind("run", "Run", Icons.AutoMirrored.Filled.DirectionsRun, "Road or trail running"),
    ActivityKind("walk", "Walk", Icons.AutoMirrored.Filled.DirectionsWalk, "Walking or power walking"),
    ActivityKind("cycle", "Cycle", Icons.AutoMirrored.Filled.DirectionsBike, "Road or indoor cycling"),
    ActivityKind("swim", "Swim", Icons.Default.Pool, "Pool or open water", gpsCapable = false),
    ActivityKind("hike", "Hike", Icons.Default.Forest, "Trail hiking or trekking"),
    ActivityKind("other", "Other", Icons.Default.FitnessCenter, "Gym, HIIT, or other activity", gpsCapable = false)
)

// ─── RecordSelectScreen ───────────────────────────────────────────────────────

@HiltViewModel
class RecordSelectViewModel @Inject constructor(
    private val liveWorkoutManager: LiveWorkoutManager
) : ViewModel() {
    suspend fun startSession(type: String, useGps: Boolean): ActivitySessionEntity {
        return liveWorkoutManager.start(type = type, useGps = useGps)
    }
}

@Composable
fun RecordSelectScreen(navController: NavController, vm: RecordSelectViewModel = hiltViewModel()) {
    val colors = LocalPulseColors.current
    val context = LocalContext.current
    var selected by remember { mutableStateOf("run") }
    var useGps by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val gpsCapable = ACTIVITY_KINDS.firstOrNull { it.type == selected }?.gpsCapable ?: true

    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch {
            val session = vm.startSession(type = selected, useGps = granted)
            navController.navigate(NavRoute.RecordLive(session.id).route)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    "CHOOSE ACTIVITY",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    letterSpacing = 1.sp
                )
            }

            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(380.dp)
                ) {
                    items(ACTIVITY_KINDS) { kind ->
                        val isSelected = kind.type == selected
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (isSelected) colors.accentSoft else colors.card)
                                .border(1.dp, if (isSelected) colors.accent else colors.borderSubtle, RoundedCornerShape(24.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { selected = kind.type }
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(if (isSelected) colors.accentSoft else colors.cardSoft, CircleShape)
                                ) {
                                    Icon(kind.icon, contentDescription = null, tint = if (isSelected) colors.accent else colors.textSecondary, modifier = Modifier.size(22.dp))
                                }
                                Text(kind.label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = colors.textPrimary)
                                Text(kind.helper, fontSize = 12.sp, color = colors.textMuted, maxLines = 2)
                            }
                        }
                    }
                }
            }

            // GPS toggle
            item {
                PulseCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Use GPS route", fontWeight = FontWeight.Medium, fontSize = 15.sp, color = colors.textPrimary)
                            Text(
                                if (gpsCapable) "Track your route on a map" else "Not available for this activity",
                                fontSize = 12.sp, color = colors.textMuted
                            )
                        }
                        Switch(
                            checked = useGps && gpsCapable,
                            onCheckedChange = { if (gpsCapable) useGps = it },
                            enabled = gpsCapable,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.accent)
                        )
                    }
                }
            }

            item {
                PrimaryButton(title = "Start", onClick = {
                    val willUseGps = useGps && gpsCapable
                    if (willUseGps) {
                        val already = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!already) {
                            locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            return@PrimaryButton
                        }
                    }
                    scope.launch {
                        val session = vm.startSession(type = selected, useGps = willUseGps)
                        navController.navigate(NavRoute.RecordLive(session.id).route)
                    }
                })
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// ─── RecordLiveViewModel ──────────────────────────────────────────────────────

@HiltViewModel
class RecordLiveViewModel @Inject constructor(
    private val liveWorkoutManager: LiveWorkoutManager,
    private val activityRepo: ActivityRepository
) : ViewModel() {
    val liveState: StateFlow<WorkoutLiveState?> = liveWorkoutManager.liveState

    private val _liveRoute = MutableStateFlow<List<LatLng>>(emptyList())
    val liveRoute: StateFlow<List<LatLng>> = _liveRoute.asStateFlow()

    fun observeSession(sessionId: String) = activityRepo.observeSessions()

    fun startRouteTracking(sessionId: String) {
        viewModelScope.launch {
            val existing = activityRepo.getGpsPointsForSession(sessionId)
                .filter { it.accepted }.sortedBy { it.timestamp }
                .map { LatLng(it.latitude, it.longitude) }
            _liveRoute.value = existing
            PulseEventBus.events.collect { event ->
                if (event is PulseEvent.GpsPoint && event.sessionId == sessionId && event.accepted) {
                    _liveRoute.value = _liveRoute.value + LatLng(event.latitude, event.longitude)
                }
            }
        }
    }

    suspend fun pause(session: ActivitySessionEntity) = liveWorkoutManager.pause(session)
    suspend fun resume(session: ActivitySessionEntity) = liveWorkoutManager.resume(session)
    suspend fun finish(session: ActivitySessionEntity) = liveWorkoutManager.finish(session)
    suspend fun cancel(session: ActivitySessionEntity) = liveWorkoutManager.cancel(session)
    suspend fun ensureActive(session: ActivitySessionEntity) = liveWorkoutManager.ensureActive(session)
}

@Composable
fun RecordLiveScreen(sessionId: String, navController: NavController, vm: RecordLiveViewModel = hiltViewModel()) {
    val colors = LocalPulseColors.current
    val liveState by vm.liveState.collectAsState()
    val sessionsFlow = remember { vm.observeSession(sessionId) }
    val sessions by sessionsFlow.collectAsState(initial = emptyList())
    val session = sessions.firstOrNull { it.id == sessionId }
    val liveRoute by vm.liveRoute.collectAsState()
    val scope = rememberCoroutineScope()
    var showFinishDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) { vm.startRouteTracking(sessionId) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (session == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accent)
            }
            return@Box
        }

        val isPaused = session.statusRaw == ActivitySessionStatus.PAUSED.rawValue

        var localElapsedSecs by remember { mutableStateOf(0) }
        LaunchedEffect(isPaused, session.startedAt, session.pausedAt, session.elapsedPausedMs) {
            if (isPaused) {
                val frozenAt = session.pausedAt ?: System.currentTimeMillis()
                localElapsedSecs = maxOf(0, ((frozenAt - session.startedAt - session.elapsedPausedMs) / 1000L).toInt())
                return@LaunchedEffect
            }
            while (true) {
                localElapsedSecs = maxOf(0, ((System.currentTimeMillis() - session.startedAt - session.elapsedPausedMs) / 1000L).toInt())
                delay(1_000L)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                PulseCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "${if (isPaused) "Paused" else "Recording"} ${activityLabel(session.activityType)}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            formatDuration(localElapsedSecs),
                            fontSize = 60.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPaused) colors.textMuted else colors.textPrimary
                        )
                        Text("DURATION", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, letterSpacing = 1.4.sp)
                    }
                }
            }

            // Live stat tiles
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (session.useGps) {
                        LiveStatTile(
                            label = "Distance",
                            value = liveState?.distanceMeters?.let { "%.2f km".format(it / 1000) } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    LiveStatTile(
                        label = "Heart rate",
                        value = liveState?.latestHR?.let { "$it bpm" } ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LiveStatTile(
                        label = "SpO2",
                        value = liveState?.latestSpO2?.let { "$it%" } ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                    if (session.useGps) {
                        LiveStatTile(
                            label = "Pace",
                            value = liveState?.paceSecondsPerKm?.let { sec ->
                                "%d:%02d /km".format((sec / 60).toInt(), (sec % 60).toInt())
                            } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Live GPS map (shown once tracking starts)
            if (session.useGps && liveRoute.isNotEmpty()) {
                item {
                    RouteMapCardLatLng(
                        latLngs = liveRoute,
                        height = 180.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Controls
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SecondaryButton(
                        title = if (isPaused) "Resume" else "Pause",
                        onClick = {
                            scope.launch {
                                if (isPaused) vm.resume(session) else vm.pause(session)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        title = "Finish",
                        onClick = { showFinishDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                SecondaryButton(
                    title = "Discard",
                    onClick = { showDiscardDialog = true }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        if (showFinishDialog) {
            AlertDialog(
                onDismissRequest = { showFinishDialog = false },
                title = { Text("Finish workout?") },
                text = { Text("Your workout will be saved with its time and ring measurements.") },
                confirmButton = {
                    TextButton(onClick = {
                        showFinishDialog = false
                        scope.launch {
                            vm.finish(session)
                            navController.navigate(NavRoute.RecordSummary(sessionId).route) {
                                popUpTo(NavRoute.Activity.route) { inclusive = false }
                            }
                        }
                    }) { Text("Finish") }
                },
                dismissButton = { TextButton(onClick = { showFinishDialog = false }) { Text("Keep recording") } },
                containerColor = colors.card
            )
        }

        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = { Text("Discard workout?") },
                text = { Text("This recording will be deleted and won't count toward your activity.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDiscardDialog = false
                        scope.launch {
                            vm.cancel(session)
                            navController.popBackStack(NavRoute.Activity.route, inclusive = false)
                        }
                    }) { Text("Discard", color = colors.danger) }
                },
                dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text("Keep recording") } },
                containerColor = colors.card
            )
        }
    }
}

@Composable
private fun LiveStatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalPulseColors.current
    PulseCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, maxLines = 1)
        }
    }
}

// ─── RecordSummaryViewModel ───────────────────────────────────────────────────

@HiltViewModel
class RecordSummaryViewModel @Inject constructor(
    private val activityRepo: ActivityRepository
) : ViewModel() {
    private val _session = MutableStateFlow<ActivitySessionEntity?>(null)
    val session: StateFlow<ActivitySessionEntity?> = _session.asStateFlow()

    private val _hrSamples = MutableStateFlow<List<space.linuxct.pulseloop.domain.model.MetricSample>>(emptyList())
    val hrSamples: StateFlow<List<space.linuxct.pulseloop.domain.model.MetricSample>> = _hrSamples.asStateFlow()

    private val _gpsPoints = MutableStateFlow<List<ActivityGpsPointEntity>>(emptyList())
    val gpsPoints: StateFlow<List<ActivityGpsPointEntity>> = _gpsPoints.asStateFlow()

    fun load(sessionId: String) {
        viewModelScope.launch {
            val session = activityRepo.getSessionById(sessionId)
            _session.value = session
            if (session != null) {
                val samples = activityRepo.getSamplesForSession(sessionId)
                _hrSamples.value = samples.filter { it.kindRaw == "hr" && it.value > 0 }.sortedBy { it.timestamp }
                    .map { space.linuxct.pulseloop.domain.model.MetricSample(it.timestamp, it.value) }
            }
        }
        viewModelScope.launch {
            activityRepo.observeGpsPointsForSession(sessionId).collect { points ->
                _gpsPoints.value = points.filter { it.accepted }
            }
        }
    }

    fun deleteSession(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val s = _session.value ?: return@launch
            activityRepo.deleteSession(s.id)
            onDeleted()
        }
    }
}

@Composable
fun RecordSummaryScreen(sessionId: String, navController: NavController, vm: RecordSummaryViewModel = hiltViewModel()) {
    val colors = LocalPulseColors.current
    val session by vm.session.collectAsState()
    val hrSamples by vm.hrSamples.collectAsState()
    val gpsPoints by vm.gpsPoints.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    remember(sessionId) { vm.load(sessionId) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (session == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accent)
            }
            return@Box
        }

        val s = session!!
        val duration = s.finishedAt?.let { ((it - s.startedAt - s.elapsedPausedMs) / 1000).toInt() }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            item {
                PulseCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .background(colors.accentSoft, CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = colors.accent, modifier = Modifier.size(36.dp))
                        }
                        Text("WORKOUT SAVED", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.accent, letterSpacing = 1.8.sp)
                        Text(activityLabel(s.activityType), fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = colors.textPrimary)
                        val dateFmt = java.text.SimpleDateFormat("EEE, MMM d · h:mm a", java.util.Locale.getDefault())
                        Text(dateFmt.format(java.util.Date(s.startedAt)), fontSize = 13.sp, color = colors.textMuted)
                    }
                }
            }

            // Hero band: distance / duration / pace
            item {
                PulseCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        if (s.useGps) {
                            StatColumn("%.2f".format(s.totalDistanceMeters / 1000), "KM")
                            Spacer(Modifier.size(1.dp))
                        }
                        StatColumn(duration?.let { formatDuration(it) } ?: "—", "DURATION")
                        if (s.totalCalories > 0) {
                            Spacer(Modifier.size(1.dp))
                            StatColumn("${s.totalCalories.toInt()}", "CALORIES")
                        }
                    }
                }
            }

            // GPS route map
            if (gpsPoints.isNotEmpty()) {
                item {
                    RouteMapCard(
                        points = gpsPoints,
                        interactive = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // HR chart
            if (hrSamples.size > 1) {
                item {
                    PulseCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("HEART RATE", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                                HrLineChart(samples = hrSamples)
                            }
                        }
                    }
                }
            }

            // Delete button
            item {
                SecondaryButton(title = "Delete Workout", onClick = { showDeleteDialog = true })
            }

            item {
                PrimaryButton(title = "Done", onClick = {
                    navController.popBackStack(NavRoute.Activity.route, inclusive = false)
                })
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete this workout?") },
                text = { Text("This permanently removes the workout and its data. This can't be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        vm.deleteSession { navController.popBackStack(NavRoute.Activity.route, inclusive = false) }
                    }) { Text("Delete", color = colors.danger) }
                },
                dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
                containerColor = colors.card
            )
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    val colors = LocalPulseColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, color = colors.textPrimary, maxLines = 1)
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, letterSpacing = 0.8.sp)
    }
}
