package space.linuxct.pulseloop.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import space.linuxct.pulseloop.ble.RingBLEClient
import space.linuxct.pulseloop.data.datastore.AppPreferencesDataStore
import space.linuxct.pulseloop.data.db.dao.ActivityDailyDao
import space.linuxct.pulseloop.data.db.dao.ActivitySessionDao
import space.linuxct.pulseloop.data.db.dao.MeasurementDao
import space.linuxct.pulseloop.data.db.dao.ProfileDao
import space.linuxct.pulseloop.data.db.dao.SleepDao
import space.linuxct.pulseloop.data.db.entities.RawPacketRowEntity
import space.linuxct.pulseloop.data.db.entities.WearableLogEntity
import space.linuxct.pulseloop.domain.model.RingConnectionState
import space.linuxct.pulseloop.domain.repository.DebugRepository
import space.linuxct.pulseloop.domain.repository.DeviceRepository
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

data class DebugUiState(
    val packets: List<RawPacketRowEntity> = emptyList(),
    val logs: List<WearableLogEntity> = emptyList(),
    val connectionState: RingConnectionState = RingConnectionState.IDLE,
    val deviceName: String? = null,
    val deviceAddress: String? = null,
    val batteryPercent: Int? = null
)

@HiltViewModel
class DebugViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val debugRepo: DebugRepository,
    private val deviceRepo: DeviceRepository,
    private val bleClient: RingBLEClient,
    private val measurementDao: MeasurementDao,
    private val activityDailyDao: ActivityDailyDao,
    private val activitySessionDao: ActivitySessionDao,
    private val sleepDao: SleepDao,
    private val profileDao: ProfileDao,
    private val prefs: AppPreferencesDataStore
) : ViewModel() {

    val uiState: StateFlow<DebugUiState> = combine(
        debugRepo.observeRecentPackets(),
        debugRepo.observeLogs(),
        bleClient.connectionState,
        bleClient.batteryPercent,
        deviceRepo.observeDevice()
    ) { packets, logs, connState, battery, device ->
        DebugUiState(
            packets = packets,
            logs = logs,
            connectionState = connState,
            deviceName = device?.name,
            deviceAddress = device?.macAddress,
            batteryPercent = battery
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebugUiState())

    fun clearAll() {
        viewModelScope.launch { debugRepo.clearAll() }
    }

    fun exportDiagnostics(onReady: (Intent) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = uiState.value
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val json = buildString {
                appendLine("{")
                appendLine("  \"exported_at\": \"$ts\",")
                appendLine("  \"ble_state\": \"${state.connectionState.rawValue}\",")
                appendLine("  \"device\": {")
                appendLine("    \"name\": ${state.deviceName?.let { "\"${it.sanitize()}\"" } ?: "null"},")
                appendLine("    \"address\": ${state.deviceAddress?.let { "\"$it\"" } ?: "null"},")
                appendLine("    \"battery\": ${state.batteryPercent ?: "null"}")
                appendLine("  },")
                appendLine("  \"packets\": [")
                state.packets.forEachIndexed { i, p ->
                    val comma = if (i < state.packets.size - 1) "," else ""
                    appendLine("    { \"ts\": ${p.timestamp}, \"dir\": \"${p.directionRaw}\", \"cmd\": \"0x%02X\".format(${p.commandId}), \"hex\": \"${p.hexPayload}\", \"device\": \"${p.deviceTypeRaw}\" }$comma")
                }
                appendLine("  ],")
                appendLine("  \"logs\": [")
                state.logs.forEachIndexed { i, l ->
                    val comma = if (i < state.logs.size - 1) "," else ""
                    appendLine("    { \"ts\": ${l.at}, \"level\": \"${l.level}\", \"category\": \"${l.category}\", \"message\": \"${l.message.sanitize()}\", \"detail\": ${l.detail?.let { "\"${it.sanitize()}\"" } ?: "null"} }$comma")
                }
                appendLine("  ]")
                append("}")
            }

            val dir = File(appContext.cacheDir, "diagnostics").also { it.mkdirs() }
            val file = File(dir, "pulseloop_debug_${System.currentTimeMillis()}.json")
            file.writeText(json)

            val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "PulseLoop diagnostics $ts")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            withContext(Dispatchers.Main) { onReady(Intent.createChooser(shareIntent, "Export diagnostics")) }
        }
    }

    fun exportFullDump(onReady: (Intent) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val workDir = File(appContext.cacheDir, "diagnostics/dump_$ts").also { it.mkdirs() }
            try {
                writeDeviceInfoJson(workDir)
                writeDiagnosticsJson(workDir, ts)
                writeMeasurementsJson(workDir)
                writeActivityJson(workDir)
                writeSleepJson(workDir)
                writeProfileJson(workDir)
                writePreferencesJson(workDir)
                writeLogcatTxt(workDir)
                copyDatabaseFiles(workDir)

                val zipFile = File(appContext.cacheDir, "diagnostics/pulseloop_dump_$ts.zip")
                zipWorkDir(workDir, zipFile)
                workDir.deleteRecursively()

                val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", zipFile)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "PulseLoop full debug dump $ts")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                withContext(Dispatchers.Main) { onReady(Intent.createChooser(shareIntent, "Share debug dump")) }
            } catch (_: Exception) {
                workDir.deleteRecursively()
            }
        }
    }

    // ── Full dump helpers ─────────────────────────────────────────────────────

    private fun writeDeviceInfoJson(dir: File) {
        val pm = appContext.packageManager
        val pi = runCatching { pm.getPackageInfo(appContext.packageName, 0) }.getOrNull()
        val versionCode = if (Build.VERSION.SDK_INT >= 28) pi?.longVersionCode ?: -1L
                          else @Suppress("DEPRECATION") pi?.versionCode?.toLong() ?: -1L
        val obj = JSONObject().apply {
            put("manufacturer", Build.MANUFACTURER)
            put("model", Build.MODEL)
            put("android_version", Build.VERSION.RELEASE)
            put("sdk_int", Build.VERSION.SDK_INT)
            put("app_version_name", pi?.versionName ?: "unknown")
            put("app_version_code", versionCode)
            put("package_name", appContext.packageName)
            put("exported_at_epoch", System.currentTimeMillis())
        }
        File(dir, "device_info.json").writeText(obj.toString(2))
    }

    private suspend fun writeDiagnosticsJson(dir: File, ts: String) {
        val state = uiState.value
        val packets = debugRepo.getAllPackets()
        val logs = debugRepo.getAllLogs()
        val obj = JSONObject().apply {
            put("exported_at", ts)
            put("ble_state", state.connectionState.rawValue)
            put("device", JSONObject().apply {
                put("name", state.deviceName ?: JSONObject.NULL)
                put("address", state.deviceAddress ?: JSONObject.NULL)
                put("battery", state.batteryPercent ?: JSONObject.NULL)
            })
            put("packets", JSONArray().also { arr ->
                packets.forEach { p ->
                    arr.put(JSONObject().apply {
                        put("ts", p.timestamp)
                        put("dir", p.directionRaw)
                        put("cmd", "0x%02X".format(p.commandId))
                        put("hex", p.hexPayload)
                        put("device", p.deviceTypeRaw)
                    })
                }
            })
            put("logs", JSONArray().also { arr ->
                logs.forEach { l ->
                    arr.put(JSONObject().apply {
                        put("ts", l.at)
                        put("level", l.level)
                        put("category", l.category)
                        put("message", l.message)
                        put("detail", l.detail ?: JSONObject.NULL)
                    })
                }
            })
        }
        File(dir, "diagnostics.json").writeText(obj.toString(2))
    }

    private suspend fun writeMeasurementsJson(dir: File) {
        val kinds = listOf("hr", "spo2", "stress", "hrv", "temp")
        val root = JSONObject()
        for (kind in kinds) {
            val rows = measurementDao.getByKind(kind)
            root.put(kind, JSONArray().also { arr ->
                rows.forEach { m ->
                    arr.put(JSONObject().apply {
                        put("id", m.id)
                        put("value", m.value)
                        put("unit", m.unit)
                        put("ts", m.timestamp)
                        put("source", m.sourceRaw)
                        put("confidence", m.confidenceRaw)
                        put("session_id", m.activitySessionId ?: JSONObject.NULL)
                    })
                }
            })
        }
        File(dir, "measurements.json").writeText(root.toString(2))
    }

    private suspend fun writeActivityJson(dir: File) {
        val daily = activityDailyDao.getAll()
        val sessions = activitySessionDao.getAll()
        val root = JSONObject().apply {
            put("daily_rows", JSONArray().also { arr ->
                daily.forEach { d ->
                    arr.put(JSONObject().apply {
                        put("id", d.id)
                        put("date", d.date)
                        put("steps", d.steps)
                        put("calories", d.calories)
                        put("distance_m", d.distanceMeters)
                        put("active_minutes", d.activeMinutes)
                        put("source", d.source)
                        put("updated_at", d.updatedAt)
                    })
                }
            })
            put("sessions", JSONArray().also { arr ->
                sessions.forEach { s ->
                    arr.put(JSONObject().apply {
                        put("id", s.id)
                        put("activity_type", s.activityType)
                        put("status", s.statusRaw)
                        put("started_at", s.startedAt)
                        put("paused_at", s.pausedAt ?: JSONObject.NULL)
                        put("finished_at", s.finishedAt ?: JSONObject.NULL)
                        put("use_gps", s.useGps)
                        put("total_steps", s.totalSteps)
                        put("total_calories", s.totalCalories)
                        put("total_distance_m", s.totalDistanceMeters)
                        put("total_active_minutes", s.totalActiveMinutes)
                        put("elapsed_paused_ms", s.elapsedPausedMs)
                        put("hr_polls", s.hrPollCount)
                        put("hr_polls_ok", s.hrPollSuccessCount)
                        put("spo2_polls", s.spo2PollCount)
                        put("spo2_polls_ok", s.spo2PollSuccessCount)
                    })
                }
            })
        }
        File(dir, "activity.json").writeText(root.toString(2))
    }

    private suspend fun writeSleepJson(dir: File) {
        val sessions = sleepDao.getAllSessions()
        val root = JSONObject().apply {
            put("sessions", JSONArray().also { arr ->
                sessions.forEach { s ->
                    val blocks = sleepDao.getBlocksForSession(s.id)
                    arr.put(JSONObject().apply {
                        put("id", s.id)
                        put("start_at", s.startAt)
                        put("end_at", s.endAt)
                        put("score", s.score ?: JSONObject.NULL)
                        put("synced_at", s.syncedAt ?: JSONObject.NULL)
                        put("device_id", s.deviceId ?: JSONObject.NULL)
                        put("blocks", JSONArray().also { bArr ->
                            blocks.forEach { b ->
                                bArr.put(JSONObject().apply {
                                    put("stage", b.stageRaw)
                                    put("start_at", b.startAt)
                                    put("duration_minutes", b.durationMinutes)
                                })
                            }
                        })
                    })
                }
            })
        }
        File(dir, "sleep.json").writeText(root.toString(2))
    }

    private suspend fun writeProfileJson(dir: File) {
        val profile = profileDao.getProfile()
        val goals = profileDao.getGoals()
        val root = JSONObject().apply {
            put("profile", if (profile == null) JSONObject.NULL else JSONObject().apply {
                put("name", profile.name ?: JSONObject.NULL)
                put("age", profile.age ?: JSONObject.NULL)
                put("height_cm", profile.heightCm ?: JSONObject.NULL)
                put("weight_kg", profile.weightKg ?: JSONObject.NULL)
                put("biological_sex", profile.biologicalSex ?: JSONObject.NULL)
                put("onboarding_completed", profile.onboardingCompleted)
                put("created_at", profile.createdAt)
                put("updated_at", profile.updatedAt)
            })
            put("goals", if (goals == null) JSONObject.NULL else JSONObject().apply {
                put("daily_steps", goals.dailySteps)
                put("sleep_minutes", goals.sleepMinutes)
                put("active_minutes", goals.activeMinutes)
                put("updated_at", goals.updatedAt)
            })
        }
        File(dir, "profile.json").writeText(root.toString(2))
    }

    private suspend fun writePreferencesJson(dir: File) {
        val hasApiKey = !prefs.openAiKey.first().isNullOrBlank()
        val hasRefreshToken = !prefs.openAiRefreshToken.first().isNullOrBlank()
        val obj = JSONObject().apply {
            put("onboarding_completed", prefs.onboardingCompleted.first())
            put("paired_device_mac", prefs.pairedDeviceMac.first() ?: JSONObject.NULL)
            put("paired_device_type", prefs.pairedDeviceType.first() ?: JSONObject.NULL)
            put("cdm_association_id", prefs.cdmAssociationId.first() ?: JSONObject.NULL)
            put("coach_enabled", prefs.coachEnabled.first())
            put("coach_model", prefs.coachModel.first() ?: JSONObject.NULL)
            put("openai_api_key_set", hasApiKey)
            put("openai_refresh_token_set", hasRefreshToken)
        }
        File(dir, "preferences.json").writeText(obj.toString(2))
    }

    private fun writeLogcatTxt(dir: File) {
        try {
            val pid = android.os.Process.myPid().toString()
            val proc = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "2000", "--pid=$pid"))
            val text = proc.inputStream.bufferedReader().readText()
            proc.waitFor()
            File(dir, "logcat.txt").writeText(text)
        } catch (_: Exception) {
            File(dir, "logcat.txt").writeText("logcat capture failed")
        }
    }

    private fun copyDatabaseFiles(dir: File) {
        val dbDir = File(dir, "database").also { it.mkdirs() }
        val dbMain = appContext.getDatabasePath("pulse_database")
        listOf(dbMain, File("${dbMain.path}-shm"), File("${dbMain.path}-wal")).forEach { src ->
            if (src.exists()) src.copyTo(File(dbDir, src.name), overwrite = true)
        }
    }

    private fun zipWorkDir(sourceDir: File, zipFile: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            sourceDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val entryName = file.relativeTo(sourceDir).path
                zos.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    private fun String.sanitize() = replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
}
