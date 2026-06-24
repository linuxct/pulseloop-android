package space.linuxct.pulseloop.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import space.linuxct.pulseloop.data.repository.ActivityRepositoryImpl
import space.linuxct.pulseloop.data.repository.CoachRepositoryImpl
import space.linuxct.pulseloop.data.repository.DebugRepositoryImpl
import space.linuxct.pulseloop.data.repository.DeviceRepositoryImpl
import space.linuxct.pulseloop.data.repository.MeasurementRepositoryImpl
import space.linuxct.pulseloop.data.repository.ProfileRepositoryImpl
import space.linuxct.pulseloop.data.repository.SleepRepositoryImpl
import space.linuxct.pulseloop.domain.repository.ActivityRepository
import space.linuxct.pulseloop.domain.repository.CoachRepository
import space.linuxct.pulseloop.domain.repository.DebugRepository
import space.linuxct.pulseloop.domain.repository.DeviceRepository
import space.linuxct.pulseloop.domain.repository.MeasurementRepository
import space.linuxct.pulseloop.domain.repository.ProfileRepository
import space.linuxct.pulseloop.domain.repository.SleepRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository
    @Binds @Singleton abstract fun bindMeasurementRepository(impl: MeasurementRepositoryImpl): MeasurementRepository
    @Binds @Singleton abstract fun bindActivityRepository(impl: ActivityRepositoryImpl): ActivityRepository
    @Binds @Singleton abstract fun bindSleepRepository(impl: SleepRepositoryImpl): SleepRepository
    @Binds @Singleton abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
    @Binds @Singleton abstract fun bindCoachRepository(impl: CoachRepositoryImpl): CoachRepository
    @Binds @Singleton abstract fun bindDebugRepository(impl: DebugRepositoryImpl): DebugRepository
}
