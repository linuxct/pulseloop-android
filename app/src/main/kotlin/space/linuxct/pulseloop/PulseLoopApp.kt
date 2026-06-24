package space.linuxct.pulseloop

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.getSystemService
import dagger.hilt.android.HiltAndroidApp
import space.linuxct.pulseloop.ble.EventPersistenceSubscriber
import space.linuxct.pulseloop.ble.RingStartupCoordinator
import space.linuxct.pulseloop.coach.CoachNotificationScheduler
import space.linuxct.pulseloop.coach.CoachSummaryCoordinator
import space.linuxct.pulseloop.diagnostics.DiagnosticsSubscriber
import javax.inject.Inject

@HiltAndroidApp
class PulseLoopApp : Application() {

    @Inject lateinit var eventPersistenceSubscriber: EventPersistenceSubscriber
    @Inject lateinit var diagnosticsSubscriber: DiagnosticsSubscriber
    @Inject lateinit var coachSummaryCoordinator: CoachSummaryCoordinator
    @Inject lateinit var coachNotificationScheduler: CoachNotificationScheduler
    @Inject lateinit var ringStartupCoordinator: RingStartupCoordinator

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        eventPersistenceSubscriber.start()
        diagnosticsSubscriber.start()
        coachSummaryCoordinator.start()
        coachNotificationScheduler.scheduleIfNeeded(this)
        ringStartupCoordinator.start()
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

        nm.createNotificationChannels(listOf(coachChannel, workoutChannel, ringChannel))
    }

    companion object {
        const val CHANNEL_COACH   = "pulseloop_coach"
        const val CHANNEL_WORKOUT = "pulseloop_workout"
        const val CHANNEL_RING    = "pulseloop_ring"
    }
}
