package space.linuxct.pulseloop

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import space.linuxct.pulseloop.ble.EventPersistenceSubscriber
import space.linuxct.pulseloop.ble.RingStartupCoordinator
import space.linuxct.pulseloop.coach.CoachNotificationScheduler
import space.linuxct.pulseloop.coach.CoachSummaryCoordinator
import space.linuxct.pulseloop.diagnostics.DiagnosticsSubscriber
import space.linuxct.pulseloop.export.OtlpExportTrigger
import space.linuxct.pulseloop.export.OtlpExportWorker
import space.linuxct.pulseloop.update.UpdateCheckWorker
import javax.inject.Inject

@HiltAndroidApp
class PulseLoopApp : Application(), Configuration.Provider {

    @Inject lateinit var eventPersistenceSubscriber: EventPersistenceSubscriber
    @Inject lateinit var diagnosticsSubscriber: DiagnosticsSubscriber
    @Inject lateinit var coachSummaryCoordinator: CoachSummaryCoordinator
    @Inject lateinit var coachNotificationScheduler: CoachNotificationScheduler
    @Inject lateinit var ringStartupCoordinator: RingStartupCoordinator
    @Inject lateinit var otlpExportTrigger: OtlpExportTrigger
    @Inject lateinit var workerFactory: HiltWorkerFactory

    // On-demand WorkManager init (the default initializer is removed in the manifest) so the
    // HiltWorkerFactory can construct @HiltWorker workers like OtlpExportWorker.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        eventPersistenceSubscriber.start()
        diagnosticsSubscriber.start()
        coachSummaryCoordinator.start()
        coachNotificationScheduler.scheduleIfNeeded(this)
        ringStartupCoordinator.start()
        UpdateCheckWorker.schedule(this)
        otlpExportTrigger.start()
        OtlpExportWorker.schedulePeriodic(this)
    }

    private fun createNotificationChannels() {
        val nm = getSystemService<NotificationManager>() ?: return

        val coachChannel = NotificationChannel(
            CHANNEL_COACH,
            getString(R.string.notification_channel_coach),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Daily health insights from your AI coach"
            enableVibration(true)
        }

        val workoutChannel = NotificationChannel(
            CHANNEL_WORKOUT,
            getString(R.string.notification_channel_workout),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Live workout progress"
            setShowBadge(false)
        }

        val ringChannel = NotificationChannel(
            CHANNEL_RING,
            getString(R.string.notification_channel_ring),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows while your ring is connected"
            setShowBadge(false)
            enableVibration(false)
            enableLights(false)
        }

        val updatesChannel = NotificationChannel(
            CHANNEL_UPDATES,
            getString(R.string.notification_channel_updates),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications when a new app version is available"
            enableVibration(false)
        }

        val healthAlertsChannel = NotificationChannel(
            CHANNEL_HEALTH_ALERTS,
            getString(R.string.notification_channel_health_alerts),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts about your ring's health monitoring status"
        }

        nm.createNotificationChannels(listOf(coachChannel, workoutChannel, ringChannel, updatesChannel, healthAlertsChannel))
    }

    companion object {
        const val CHANNEL_COACH          = "pulseloop_coach"
        const val CHANNEL_WORKOUT        = "pulseloop_workout"
        const val CHANNEL_RING           = "pulseloop_ring"
        const val CHANNEL_UPDATES        = "pulseloop_updates"
        const val CHANNEL_HEALTH_ALERTS  = "pulseloop_health_alerts"
    }
}
