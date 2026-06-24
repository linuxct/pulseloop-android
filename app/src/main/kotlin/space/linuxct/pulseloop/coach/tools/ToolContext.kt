package space.linuxct.pulseloop.coach.tools

import space.linuxct.pulseloop.data.db.dao.ActivityDailyDao
import space.linuxct.pulseloop.data.db.dao.ActivitySessionDao
import space.linuxct.pulseloop.data.db.dao.CoachDao
import space.linuxct.pulseloop.data.db.dao.MeasurementDao
import space.linuxct.pulseloop.data.db.dao.ProfileDao
import space.linuxct.pulseloop.data.db.dao.SleepDao

data class ToolContext(
    val measurementDao: MeasurementDao,
    val activityDailyDao: ActivityDailyDao,
    val sleepDao: SleepDao,
    val activitySessionDao: ActivitySessionDao,
    val profileDao: ProfileDao,
    val coachDao: CoachDao
)
