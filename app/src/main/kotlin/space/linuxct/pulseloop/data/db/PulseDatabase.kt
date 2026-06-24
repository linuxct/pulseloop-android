package space.linuxct.pulseloop.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import space.linuxct.pulseloop.data.db.dao.ActivityDailyDao
import space.linuxct.pulseloop.data.db.dao.ActivitySessionDao
import space.linuxct.pulseloop.data.db.dao.CoachDao
import space.linuxct.pulseloop.data.db.dao.DebugDao
import space.linuxct.pulseloop.data.db.dao.DeviceDao
import space.linuxct.pulseloop.data.db.dao.MeasurementDao
import space.linuxct.pulseloop.data.db.dao.ProfileDao
import space.linuxct.pulseloop.data.db.dao.SleepDao
import space.linuxct.pulseloop.data.db.entities.ActivityDailyEntity
import space.linuxct.pulseloop.data.db.entities.ActivityEventEntity
import space.linuxct.pulseloop.data.db.entities.ActivityGpsPointEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySampleEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySensorPollEventEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySessionEntity
import space.linuxct.pulseloop.data.db.entities.CoachConversationEntity
import space.linuxct.pulseloop.data.db.entities.CoachMemoryEntity
import space.linuxct.pulseloop.data.db.entities.CoachMessageEntity
import space.linuxct.pulseloop.data.db.entities.CoachNotificationRecordEntity
import space.linuxct.pulseloop.data.db.entities.CoachSummaryEntity
import space.linuxct.pulseloop.data.db.entities.CoachToolCallEntity
import space.linuxct.pulseloop.data.db.entities.DerivedUpdateRowEntity
import space.linuxct.pulseloop.data.db.entities.DeviceEntity
import space.linuxct.pulseloop.data.db.entities.MeasurementEntity
import space.linuxct.pulseloop.data.db.entities.RawPacketRowEntity
import space.linuxct.pulseloop.data.db.entities.SleepSessionEntity
import space.linuxct.pulseloop.data.db.entities.SleepStageBlockEntity
import space.linuxct.pulseloop.data.db.entities.UserGoalEntity
import space.linuxct.pulseloop.data.db.entities.UserProfileEntity
import space.linuxct.pulseloop.data.db.entities.WearableLogEntity

@Database(
    entities = [
        DeviceEntity::class,
        ActivityDailyEntity::class,
        MeasurementEntity::class,
        SleepSessionEntity::class,
        SleepStageBlockEntity::class,
        RawPacketRowEntity::class,
        DerivedUpdateRowEntity::class,
        UserProfileEntity::class,
        UserGoalEntity::class,
        ActivitySessionEntity::class,
        ActivitySampleEntity::class,
        ActivityGpsPointEntity::class,
        ActivitySensorPollEventEntity::class,
        ActivityEventEntity::class,
        CoachConversationEntity::class,
        CoachMessageEntity::class,
        CoachMemoryEntity::class,
        CoachToolCallEntity::class,
        CoachNotificationRecordEntity::class,
        CoachSummaryEntity::class,
        WearableLogEntity::class,
    ],
    version = 3,
    exportSchema = true
)
abstract class PulseDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun activityDailyDao(): ActivityDailyDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun sleepDao(): SleepDao
    abstract fun profileDao(): ProfileDao
    abstract fun activitySessionDao(): ActivitySessionDao
    abstract fun coachDao(): CoachDao
    abstract fun debugDao(): DebugDao
}
