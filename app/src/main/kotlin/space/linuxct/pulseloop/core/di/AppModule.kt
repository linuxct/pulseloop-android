package space.linuxct.pulseloop.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import space.linuxct.pulseloop.data.db.MIGRATION_1_2
import space.linuxct.pulseloop.data.db.MIGRATION_2_3
import space.linuxct.pulseloop.data.db.MIGRATION_3_4
import space.linuxct.pulseloop.data.db.MIGRATION_4_5
import space.linuxct.pulseloop.data.db.PulseDatabase
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pulse_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PulseDatabase =
        Room.databaseBuilder(context, PulseDatabase::class.java, "pulse_database")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides fun provideDeviceDao(db: PulseDatabase) = db.deviceDao()
    @Provides fun provideActivityDailyDao(db: PulseDatabase) = db.activityDailyDao()
    @Provides fun provideMeasurementDao(db: PulseDatabase) = db.measurementDao()
    @Provides fun provideSleepDao(db: PulseDatabase) = db.sleepDao()
    @Provides fun provideProfileDao(db: PulseDatabase) = db.profileDao()
    @Provides fun provideActivitySessionDao(db: PulseDatabase) = db.activitySessionDao()
    @Provides fun provideCoachDao(db: PulseDatabase) = db.coachDao()
    @Provides fun provideDebugDao(db: PulseDatabase) = db.debugDao()
    @Provides fun provideOtlpExportStateDao(db: PulseDatabase) = db.otlpExportStateDao()
}
